package com.sisal.transaction.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.sisal.transaction.server.test.controller",
        "com.sisal.transaction.server.controller",
        "com.sisal.transaction.server.model.rest",
        "com.sisal.transaction.server.delegate",
        "com.sisal.transaction.server.config",
        "com.sisal.transaction.server.exception"})
public class AppStarter extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(AppStarter.class, args);
    }

    /**
     * Customizes the name of the property file loaded.
     *
     * @param application
     * @return
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        Map<String, Object> props = new HashMap<>();
        props.put("spring.config.name", "transaction.spring");
        application.properties(props);
        return application;
    }
}
