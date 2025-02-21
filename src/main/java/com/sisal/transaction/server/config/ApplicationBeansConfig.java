package com.sisal.transaction.server.config;


import com.sisal.transaction.server.util.TransactionMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationBeansConfig {
    /**
     * Mapper Beans
     */
    @Bean
    public TransactionMapper transactionMapper() {
        return new TransactionMapper();
    }

}
