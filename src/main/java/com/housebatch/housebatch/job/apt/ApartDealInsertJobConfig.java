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
            , Step aptDealInsertStep
    ) {
        return jobBuilderFactory.get("aptDealInsertJob")
                .incrementer(new RunIdIncrementer())
                //.validator(new FilePathParameterValidator())
                .validator(aptDealJobParameterValidator())
                .start(guLawdCdStep)
                .next(contextPrintStep)
                .next(aptDealInsertStep)
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

            List<String> guLawdCds = lawdRepository.findDistinctGuLawdCd();
            executionContext.putString("guLawdCd", guLawdCds.get(0));

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
