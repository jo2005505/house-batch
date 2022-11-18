package com.housebatch.housebatch.job.apt;

import com.housebatch.housebatch.adapter.ApartmentApiResource;
import com.housebatch.housebatch.core.dto.AptDealDto;
import com.housebatch.housebatch.core.repository.LawdRepository;
import com.housebatch.housebatch.core.service.AptDealService;
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

    @Bean
    public Job aptDealInsertJob(
            Step guLawdCdStep
            , Step aptDealInsertStep
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
                .validator(new YearMonthParameterValidator())
                .start(guLawdCdStep)
                .on("CONTINUABLE").to(aptDealInsertStep).next(guLawdCdStep)  // ExitStatus가 CONTINUABLE로 존재하면 contextPrintStep, guLawdCdStep를 수행
                .from(guLawdCdStep)                                                // ExitStatus가 CONTINUABLE이 아니면 종료
                .on("*").end()
                .end()
//                .next(aptDealInsertStep)
                .build();
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
    public Tasklet guLawdCdTasklet(LawdRepository lawdRepository) {
        return new GuLawdTasklet(lawdRepository);
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
                .<AptDealDto, AptDealDto>chunk(100)
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
    public ItemWriter<AptDealDto> aptDealWriter(AptDealService aptDealService) {
        return items -> {
            items.forEach(aptDealService::upsert);
            items.forEach(System.out::println);
            System.out.println("========================== Writing Completed =============================");
        };
    }
}
