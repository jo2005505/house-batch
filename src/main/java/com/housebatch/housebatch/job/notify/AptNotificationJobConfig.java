package com.housebatch.housebatch.job.notify;

import com.housebatch.housebatch.adapter.FakeSendService;
import com.housebatch.housebatch.core.dto.AptDto;
import com.housebatch.housebatch.core.dto.NotificationDto;
import com.housebatch.housebatch.core.entity.AptNotification;
import com.housebatch.housebatch.core.repository.AptNotificationRepository;
import com.housebatch.housebatch.core.repository.LawdRepository;
import com.housebatch.housebatch.core.service.AptDealService;
import com.housebatch.housebatch.job.validator.DealDateParameterValidator;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AptNotificationJobConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job aptNotificationJob(
            Step aptNotificationStep
    ) {
        return jobBuilderFactory.get("aptNotificationJob")
                .incrementer(new RunIdIncrementer())
                .validator(new DealDateParameterValidator())
                .start(aptNotificationStep)
                .build();
    }

    @JobScope
    @Bean
    public Step aptNotificationStep(
            RepositoryItemReader<AptNotification> aptNotificationRepositoryItemReader
            , ItemProcessor<AptNotification, NotificationDto> aptNotificationProcessor
            , ItemWriter<NotificationDto> aptNotificationWriter
    ) {
        return stepBuilderFactory.get("aptNotificationStep")
                .<AptNotification, NotificationDto>chunk(10)
                .reader(aptNotificationRepositoryItemReader)
                .processor(aptNotificationProcessor)
                .writer(aptNotificationWriter)
                .build();
    }

    /**
     * RepositoryItemReader Class??? Spring Batch ItemReader ???????????? ?????????????????? ????????? ????????? ?????? ?????? ????????????.
     * ?????? Service ?????? Repository??? ???????????? ???????????????, Chunk ????????? ???????????? ?????? RepositoryItemReader??? ????????????.
     *
     * [??????????????? ????????? ?????????]
     * https://hyowong.tistory.com/92
     * https://renuevo.github.io/spring/batch/spring-batch-chapter-3/
     * https://techblog.woowahan.com/2662/
     * @return
     */
    @Bean
    @StepScope
    public RepositoryItemReader<AptNotification> aptNotificationRepositoryItemReader(
            AptNotificationRepository aptNotificationRepository
    ) {
        return new RepositoryItemReaderBuilder<AptNotification>()
                .name("aptNotificationRepositoryItemReader")
                .repository(aptNotificationRepository)
                .methodName("findByEnabledIsTrue")
                .pageSize(10)
                .arguments(Arrays.asList())
                .sorts(Collections.singletonMap("aptNotificationId", Sort.Direction.DESC))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<AptNotification, NotificationDto> aptNotificationProcessor(
            @Value("#{jobParameters['dealDate']}") String dealDate
            , AptDealService aptDealService
            , LawdRepository lawdRepository
    ) {
        return aptNotification -> {

            // Spring Batch ???????????? ????????? dealDate??? guLawdCd??? ???????????? ?????? ?????? ??????
            List<AptDto> aptDtoList = aptDealService.findByGuLawdCdAndDealDate(aptNotification.getGuLawdCd(), LocalDate.parse(dealDate));

            if (aptDtoList.isEmpty()) {
                return null;
            }

            // ??? ?????? ??????
            String guName = lawdRepository.findByLawdCd(aptNotification.getGuLawdCd() + "00000")
                    .orElseThrow().getLawdDong();

            return NotificationDto.builder()
                    .email(aptNotification.getEmail())
                    .guName(guName)
                    .count(aptDtoList.size())
                    .aptDeals(aptDtoList)
                    .build();
        };
    }

    @Bean
    @StepScope
    public ItemWriter<NotificationDto> aptNotificationWriter(FakeSendService fakeSendService) {
        return items -> {
            items.forEach(item -> fakeSendService.send(item.getEmail(), item.toMessage()));
        };
    }
}
