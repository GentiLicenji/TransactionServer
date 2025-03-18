package com.sisal.transaction.test.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@TestConfiguration
public class TestConfig {

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:h2:mem:testdb;INIT=CREATE SCHEMA IF NOT EXISTS TRANSACTION_SYSTEM")
                .driverClassName("org.h2.Driver")
                .username("sa")
                .password("")
                .build();
    }

    @Bean
    @Primary
    public ListAppender<ILoggingEvent> memoryAppender() {
        ListAppender<ILoggingEvent> memoryAppender = new ListAppender<>();
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        memoryAppender.start();
        logger.addAppender(memoryAppender);
        return memoryAppender;
    }
}