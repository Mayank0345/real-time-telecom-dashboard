package com.telecom.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TelecomDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelecomDashboardApplication.class, args);
    }
}
