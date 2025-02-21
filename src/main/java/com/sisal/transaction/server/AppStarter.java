package com.sisal.transaction.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.sisal.transaction.server.controller",
        "com.sisal.transaction.server.delegate",
        "com.sisal.transaction.server.service",
        "com.sisal.transaction.server.model.rest",
        "com.sisal.transaction.server.repository",
        "com.sisal.transaction.server.exception",
        "com.sisal.transaction.server.config"})
@EntityScan("com.sisal.transaction.server.model.db")
@EnableJpaRepositories("com.sisal.transaction.server.repository")
public class AppStarter extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(AppStarter.class, args);
    }
}
