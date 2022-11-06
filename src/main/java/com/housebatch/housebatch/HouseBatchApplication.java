package com.housebatch.housebatch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableBatchProcessing	// Batch 사용하기 때문에 필요한 Annotation
@SpringBootApplication
public class HouseBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(HouseBatchApplication.class, args);
	}

}
