package com.nutricare.nutricarebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.nutricare.nutricarebackend.repository")
@EnableMongoRepositories(basePackages = "com.nutricare.nutricarebackend.mongo.repository")
public class NutricareBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(NutricareBackendApplication.class, args);
    }
}
