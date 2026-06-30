package com.geosun.rmpd.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.geosun.rmpd")
@EntityScan("com.geosun.rmpd.domain.model")
@EnableJpaRepositories("com.geosun.rmpd.infrastructure.persistence")
public class RmpdApplication {

    public static void main(String[] args) {
        SpringApplication.run(RmpdApplication.class, args);
    }
}
