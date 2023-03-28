package com.example.batchprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @EnableBatchProcessing // BatchAutoConfiguration 등록이 안되게 하여, JobLauncherApplicationRunner 가 실행되지 않음.
@SpringBootApplication
public class BatchProcessingApplication {

	public static void main(String[] args) {
		//System.exit(SpringApplication.exit(SpringApplication.run(BatchProcessingApplication.class, args)));
		SpringApplication.run(BatchProcessingApplication.class, args);
	}

}
