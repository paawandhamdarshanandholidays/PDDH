package com.pddh.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.pddh", nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EntityScan("com.pddh")
@EnableJpaRepositories(basePackages = "com.pddh", nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@ConfigurationPropertiesScan("com.pddh")
@EnableAsync
@EnableScheduling
public class PddhBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(PddhBackendApplication.class, args);
	}
}
