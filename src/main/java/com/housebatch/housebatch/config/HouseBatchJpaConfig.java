package com.housebatch.housebatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// 생성/수정 시각을 자동으로 설정을 위해 구성(@EnableJpaAuditing)
@Configuration
@EnableJpaAuditing
public class HouseBatchJpaConfig {
}
