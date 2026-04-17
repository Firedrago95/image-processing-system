package com.realteeth.assignment.global.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.resilience.annotation.EnableResilientMethods;

@Configuration
@EnableResilientMethods
public class RetryConfig {

    @Bean
    public RetryTemplate externalApiRetryTemplate() {
        RetryPolicy retryPolicy = RetryPolicy.builder()
            .maxRetries(2)
            .delay(Duration.ofSeconds(1))
            .multiplier(2.0)
            .maxDelay(Duration.ofSeconds(10))
            .build();

        return new RetryTemplate(retryPolicy);
    }
}
