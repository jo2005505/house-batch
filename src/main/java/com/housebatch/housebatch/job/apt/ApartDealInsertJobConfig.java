package com.housebatch.housebatch.job.apt;

import com.housebatch.housebatch.adapter.ApartmentApiResource;
import com.housebatch.housebatch.core.dto.AptDealDto;
import com.housebatch.housebatch.core.repository.LawdRepository;
import com.housebatch.housebatch.job.validator.FilePathParameterValidator;
import com.housebatch.housebatch.job.validator.LawdCdParameterValidator;
import com.housebatch.housebatch.job.validator.YearMonthParameterValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.CompositeJobParametersValidator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
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
//            Step aptDealInsertStep
    ) {
        return jobBuilderFactory.get("aptDealInsertJob")
                .incrementer(new RunIdIncrementer())
                //.validator(new FilePathParameterValidator())
                //.validator(aptDealJobParameterValidator())
                .start(guLawdCdStep)
                .build();
    }

    /**
     * Spring Batch에서 다수의 Class파일을 사용한 유효성 체크가 필요한 경우 CompositeJobParametersValidator로 배열에 담아 넘겨주는 기능을 구성한 Method이다.
     * @return CompositeJobParametersValidator
     */
    private JobParametersValidator aptDealJobParameterValidator() {
        CompositeJobParametersValidator validator = new CompositeJobParametersValidator();
        validator.setValidators(Arrays.asList(
                new YearMonthParameterValidator(),
                new LawdCdParameterValidator()
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
            lawdRepository.findDistinctGuLawdCd()
                    .forEach(System.out::println);
            return RepeatStatus.FINISHED;
        };
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
            , @Value("#{jobParameters['lawdCd']}") String lawdCd
            , Jaxb2Marshaller jaxb2Marshaller
    ) {
        return new StaxEventItemReaderBuilder<AptDealDto>()
                .name("aptDealResourceReader")
                //.resource(new ClassPathResource(filePath))  // Sample Filed을 읽어 정상동작되는지 코드 테스트용
                .resource(apartmentApiResource.getResource(lawdCd, YearMonth.parse(yearMonth)))
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
