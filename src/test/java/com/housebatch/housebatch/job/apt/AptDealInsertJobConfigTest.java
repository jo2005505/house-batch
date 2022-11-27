package com.housebatch.housebatch.job.apt;

import com.housebatch.housebatch.BatchTestConfig;
import com.housebatch.housebatch.adapter.ApartmentApiResource;
import com.housebatch.housebatch.core.repository.LawdRepository;
import com.housebatch.housebatch.core.service.AptDealService;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Map;

@SpringBatchTest
@SpringBootTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {ApartDealInsertJobConfig.class, BatchTestConfig.class})
public class AptDealInsertJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @MockBean
    private AptDealService aptDealService;
    @MockBean
    private LawdRepository lawdRepository;
    @MockBean
    private ApartmentApiResource apartmentApiResource;

    @Test
    public void success() throws Exception {
        // Given
        when(lawdRepository.findDistinctGuLawdCd()).thenReturn(Arrays.asList("11110"));
        // ApartmentApiResource.java의 반환 리소스 결과값으로 test-api-response.xml 이 된다.
        when(apartmentApiResource.getResource(anyString(), any())).thenReturn(new ClassPathResource("test-api-response.xml"));

        // When
        JobExecution execution = jobLauncherTestUtils.launchJob(
                new JobParameters(Maps.newHashMap("yearMonth", new JobParameter("2022-09"))));

        // Then
        Assertions.assertEquals(execution.getExitStatus(), ExitStatus.COMPLETED);
        verify(aptDealService, times(2)).upsert(any());
    }

    @Test
    public void fail_whenYearMonthNotExist() throws Exception {
        // Given
        when(lawdRepository.findDistinctGuLawdCd()).thenReturn(Arrays.asList("11110"));
        when(apartmentApiResource.getResource(anyString(), any())).thenReturn(new ClassPathResource("test-api-response.xml"));

        // When
        Assertions.assertThrows(JobParametersInvalidException.class,
                () -> jobLauncherTestUtils.launchJob());

        // Then
        verify(aptDealService, never()).upsert(any());
    }
}
