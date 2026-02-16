package com.example.Assignment_2;

import com.example.Assignment_2.orchestrator.FanOutOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@Slf4j
@SpringBootApplication
public class Assignment2Application {

	public static void main(String[] args) {
		log.info("Starting Assignment 2 - Distributed Data Fan-Out & Transformation Engine");
		
		// Run Spring Boot application
		ApplicationContext context = SpringApplication.run(Assignment2Application.class, args);
		
		// Get the orchestrator and start processing
		try {
			FanOutOrchestrator orchestrator = context.getBean(FanOutOrchestrator.class);
			orchestrator.start();
		} catch (Exception e) {
			log.error("Fatal error in fan-out orchestrator", e);
			System.exit(1);
		}
		
		log.info("Assignment 2 completed successfully");
		System.exit(0);
	}

}
