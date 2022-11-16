package com.housebatch.housebatch.job.apt;

import com.housebatch.housebatch.adapter.ApartmentApiResource;
import com.housebatch.housebatch.core.dto.AptDealDto;
import com.housebatch.housebatch.core.repository.LawdRepository;
import com.housebatch.housebatch.job.validator.FilePathParameterValidator;
import com.housebatch.housebatch.job.validator.LawdCdParameterValidator;
import com.housebatch.housebatch.job.validator.YearMonthParameterValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.CompositeJobParametersValidator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApartDealInsertJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final ApartmentApiResource apartmentApiResource;
    private final LawdRepository lawdRepository;

    @Bean
    public Job aptDealInsertJob(
            Step guLawdCdStep
            , Step contextPrintStep
//            , Step aptDealInsertStep
    ) {
        /*
         * Conditional Flow
         * - 첫 Step의 실행 결과에 따라 다음 Step을 결정하는 분기처리 기능을 제공한다.
         *
         * [기본 코드 구조]
         * 	return jobBuilderFactory.get("jobExample")
		 *	  .start(stepA())                            // 첫 Step A를 실행
		 *	  .on("*").to(stepB())                       // 다음 Step B를 실행
		 *	  .from(stepA()).on("FAILED").to(stepC())    // 다만, Step A의 결과가 "FAILED"인 경우 Step C를 실행
		 *	  .end()                                     // Conditional Flow 의 종료
		 *	  .build();
         */
        return jobBuilderFactory.get("aptDealInsertJob")
                .incrementer(new RunIdIncrementer())
                //.validator(new FilePathParameterValidator())
                .validator(aptDealJobParameterValidator())
                .start(guLawdCdStep)
                .on("CONTINUABLE").to(contextPrintStep).next(guLawdCdStep)  // ExitStatus가 CONTINUABLE로 존재하면 contextPrintStep, guLawdCdStep를 수행
                .from(guLawdCdStep)                                                // ExitStatus가 CONTINUABLE이 아니면 종료
                .on("*").end()
                .end()
//                .next(aptDealInsertStep)
                .build();
    }

    /**
     * Spring Batch에서 다수의 Class파일을 사용한 유효성 체크가 필요한 경우 CompositeJobParametersValidator로 배열에 담아 넘겨주는 기능을 구성한 Method이다.
     * @return CompositeJobParametersValidator
     */
    private JobParametersValidator aptDealJobParameterValidator() {
        CompositeJobParametersValidator validator = new CompositeJobParametersValidator();
        validator.setValidators(Arrays.asList(
                new YearMonthParameterValidator()
                //, new LawdCdParameterValidator()
        ));

        return validator;
    }

    /**
     * 공공데이터에서 가져와 데이터베이스에 저장한 아파트 실거래가 정보를 호출하여 사용하기 위한 메소드
     * @return
     */
    @Bean
    @JobScope
    public Step guLawdCdStep(
            Tasklet guLawdCdTasklet
    ) {
        return stepBuilderFactory.get("guLawdCdStep")
                .tasklet(guLawdCdTasklet)
                .build();
    }

    /**
     * ExecutionContext에 저장할 데이터
     * 1. guLawdCd : 법정동 구 코드로 다음 스텝에서 사용할 값
     * 2. guLawdCdList : 법정동 구 코드 배열
     * 3. itemCount : 남아있는 아이템(구 코드)의 개수
     */
    @Bean
    @StepScope
    public Tasklet guLawdCdTasklet() {
        return (contribution, chunkContext) -> {
            /*
             * ExecutionContext 사용
             * 사용하는 경우
             *   1. 중단된 배치 작업을 이어서 진행하기 위한 경우
             *   2. Step 간 데이터를 전달이 필요한 경우
             * 
             * 동작 방식
             *   - Spring Batch 실행내역을 Batch 관련 테이블에 메타데이터로 저장
             *     (데이터베이스 테이블 명 : BATCH_JOB_EXECUTION_CONTEXT)
             *   - Spring Batch 메타데이터 테이블을 참조하여 필요 데이터 조회
             */
            StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
            ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();

            /*
             * 데이터가 존재하면 다음 스텝을 실행, 없으면 종료되도록 진행
             *   - 데이터가 있는 경우 RepeatStatus.CONTINUABLE 로 동작
             * 
             * ExecutionContext에 저장할 데이터
             * 1. guLawdCd : 법정동 구 코드로 다음 스텝에서 사용할 값
             * 2. guLawdCdList : 법정동 구 코드 배열
             * 3. itemCount : 남아있는 아이템(구 코드)의 개수
             *
             * [고민해 볼 사항]
             * ExecutionContext에 데이터베이스에서 불러온 데이터 전체를 저장하는 방식으로 동작
             * - 매번 데이터베이스 조회하는 방식으로 작성
             *   - 단점 : 데이터베이스 조회하는 반복 동작으로 시스템 부하 발생
             *   - 장점 : 대용량의 데이터를 분할 조회하여 자원을 안정적/효율적으로 사용 가능
             *           (내부에서 제한적으로 사용하는 경우인지, 다수의 고객으로 접근이 많이 발생하는 데이터인 경우인지 고려 필요)
             */
            List<String> guLawdCdList = null;
            if(!executionContext.containsKey("guLawdCdList")) {
                guLawdCdList = lawdRepository.findDistinctGuLawdCd();
                executionContext.put("guLawdCdList", guLawdCdList);
                executionContext.putInt("itemCount", guLawdCdList.size());
            } else {
                guLawdCdList = (List<String>) executionContext.get("guLawdCdList");
            }

            Integer itemCount = executionContext.getInt("itemCount");

            if (itemCount == 0) {
                contribution.setExitStatus(ExitStatus.COMPLETED);
                return RepeatStatus.FINISHED;
            }

            String guLawdCd = guLawdCdList.get(itemCount - 1);
            executionContext.putString("guLawdCd", guLawdCd);
            executionContext.putInt("itemCount", itemCount - 1);

            contribution.setExitStatus(new ExitStatus("CONTINUABLE"));
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    @JobScope
    public Step contextPrintStep(
            Tasklet contextPrintTasklet
    ) {
        return stepBuilderFactory.get("contextPrintStep")
                .tasklet(contextPrintTasklet)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet contextPrintTasklet(
            @Value("#{jobExecutionContext['guLawdCd']}") String guLawdCd    // jobExecutionContext로 메타데이터 테이블에서 조회 가능
    ) {
        return ((contribution, chunkContext) -> {
            System.out.println("[contextPrintStep] guLawdCd ::: " + guLawdCd);

            return RepeatStatus.FINISHED;
        });
    }

    @Bean
    @JobScope
    public Step aptDealInsertStep(
            StaxEventItemReader<AptDealDto> aptDealResourceReader
            , ItemWriter<AptDealDto> aptDealWriter
    ) {
        return stepBuilderFactory.get("aptDealInsertStep")
                .<AptDealDto, AptDealDto>chunk(10)
                .reader(aptDealResourceReader)
                .writer(aptDealWriter)
                .build();
    }

    @Bean
    @StepScope
    public StaxEventItemReader<AptDealDto> aptDealResourceReader(
            // @Value("#{jobParameters['filePath']}") String filePath,  // 테스트를 위해 Sample Filed 읽을 경우 사용
            @Value("#{jobParameters['yearMonth']}") String yearMonth
            //, @Value("#{jobParameters['lawdCd']}") String lawdCd      // Spring Batch 실행 시 입력된 파라미터를 사용
            , @Value("#{jobExecutionContext['guLawdCd']}") String guLawdCd  // ExecutionContext 데이터베이스에 저장되어있는 메타데이터를 사용
            , Jaxb2Marshaller jaxb2Marshaller
    ) {
        return new StaxEventItemReaderBuilder<AptDealDto>()
                .name("aptDealResourceReader")
                //.resource(new ClassPathResource(filePath))  // Sample Filed을 읽어 정상동작되는지 코드 테스트용
                .resource(apartmentApiResource.getResource(guLawdCd, YearMonth.parse(yearMonth)))
                .addFragmentRootElements("item")  // 읽을 Root Element name 설정
                .unmarshaller(jaxb2Marshaller)    // 파일을 객체에 맵핑하는 설정
                .build();
    }

    @Bean
    @StepScope
    public Jaxb2Marshaller aptDealDtoMarshaller() {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setClassesToBeBound(AptDealDto.class);
        return jaxb2Marshaller;
    }

    @Bean
    @StepScope
    public ItemWriter<AptDealDto> aptDealWriter() {
        return items -> {
            items.forEach(System.out::println);
            System.out.println("========================== Writing Completed =============================");
        };
    }
}
