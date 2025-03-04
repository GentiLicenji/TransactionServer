package com.sisal.transaction.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@ConfigurationProperties(prefix = "transaction.rate-limiting")
@Component
public class RateLimitProperties {

    private boolean enabled = true;  // default to true

    @Min(value = 1, message = "Max transactions per minute must be at least 1")
    @Max(value = 1000, message = "Max transactions per minute cannot exceed 1000")
    private int maxPerMinute = 60;   // default value

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxPerMinute() {
        return maxPerMinute;
    }

    public void setMaxPerMinute(int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }
}
