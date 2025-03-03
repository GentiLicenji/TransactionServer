package com.sisal.transaction.server.config.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Auth property config.
 */

@Component
@ConfigurationProperties(prefix = "api.security")
public class ApiKeyProperties {
    private List<ApiClient> clients = new ArrayList<>();

    public List<ApiClient> getClients() {
        return clients;
    }

    public void setClients(List<ApiClient> clients) {
        this.clients = clients;
    }

    public ApiClient getClientByApiKey(String apiKey) {
        return clients.stream()
                .filter(client -> client.getApiKey().equals(apiKey))
                .findFirst()
                .orElse(null);
    }

    // Inner class to map properties
    public static class ApiClient {
        private String name;
        private String apiKey;
        private String secretKey;
        private List<String> roles = new ArrayList<>();

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}