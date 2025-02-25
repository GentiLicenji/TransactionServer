package com.sisal.transaction.server.controller;

import com.sisal.transaction.test.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test Suite will test OTB features in spring boot (the health check actuator).
 */
@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")//Loads application-test.properties
@AutoConfigureMockMvc(addFilters = false)//Disables all filters including the Security filter chain
public class HealthCheckActuatorIntegrationTest {

    private static final String PATH = "/actuator/health";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenAppContextStartsUP_thenReturnUpStatus() throws Exception {

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}