package com.housebatch.housebatch.job.zigbang;

import com.housebatch.housebatch.adapter.ZigBangAptIdApiResource;
import com.housebatch.housebatch.adapter.ZigBangAptInfoApiResource;
import com.housebatch.housebatch.core.dto.ZigBangDto;
import com.housebatch.housebatch.core.entity.Apt;
import com.housebatch.housebatch.core.entity.ZigBang;
import com.housebatch.housebatch.core.repository.AptRepository;
import com.housebatch.housebatch.core.service.ZigBangService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

import java.util.Collections;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ZigBangInfoJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final ZigBangService zigBangService;

    @Bean
    public Job zigBnagInfoJob(
            Step zigBangInfoStep
    ) {
        return jobBuilderFactory.get("zigBnagInfoJob")
                .incrementer(new RunIdIncrementer())
                .start(zigBangInfoStep)
                .build();
    }

    @Bean
    @JobScope
    public Step zigBangInfoStep(
            RepositoryItemReader<Apt> aptRepositoryItemReader
            , ItemProcessor<Apt, ZigBangDto> aptAndZigBangInfoMapptingProcessor
            , ItemWriter<ZigBangDto> zigBangWriter
    ) {
        return stepBuilderFactory.get("zigBangInfoStep")
                .<Apt, ZigBangDto>chunk(100)
                .reader(aptRepositoryItemReader)
                .processor(aptAndZigBangInfoMapptingProcessor)
                .writer(zigBangWriter)
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<Apt> aptRepositoryItemReader(
            AptRepository aptRepository
    ) {
        log.info("======= APT TABLE READ =======");
        return new RepositoryItemReaderBuilder<Apt>()
                .name("aptRepositoryItemReader")
                .repository(aptRepository)
                .methodName("findAll")
                .pageSize(100)
                .sorts(Collections.singletonMap("aptId", Sort.Direction.ASC))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Apt, ZigBangDto> aptAndZigBangInfoMapptingProcessor(
            ZigBangAptIdApiResource zigBangAptIdApiResource
            , ZigBangAptInfoApiResource zigBangAptInfoApiResource
    ) {
        return new AptAndZigBangInfoMapptingProcessor(zigBangAptIdApiResource, zigBangAptInfoApiResource);
    }

    @Bean
    @StepScope
    public ItemWriter<ZigBangDto> zigBangWriter() {
        return items -> {
            items.forEach(zigBangService::upsert);
        };
    }
}
