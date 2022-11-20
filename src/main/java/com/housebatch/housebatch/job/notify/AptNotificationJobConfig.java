package com.housebatch.housebatch.job.notify;

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
     * RepositoryItemReader Class는 Spring Batch ItemReader 부분에서 데이터베이스 조회가 필요할 경우 기능 지원한다.
     * 직접 Service 또는 Repository를 호출해도 가능하지만, Chunk 기능을 사용하기 위해 RepositoryItemReader를 권장한다.
     *
     * [찾아보면서 참고한 사이트]
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

            // Spring Batch 파라미터 요소인 dealDate와 guLawdCd와 일치하는 거래 내역 조회
            List<AptDto> aptDtoList = aptDealService.findByGuLawdCdAndDealDate(aptNotification.getGuLawdCd(), LocalDate.parse(dealDate));

            if (aptDtoList.isEmpty()) {
                return null;
            }

            // 구 명칭 조회
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
    public ItemWriter<NotificationDto> aptNotificationWriter() {
        return items -> {
            items.forEach(item -> System.out.println(item.toMessage()));
        };
    }
}
