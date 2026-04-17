package com.realteeth.assignment.worker;

import com.realteeth.assignment.worker.dto.request.ProcessRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MockWorkerClient {

    private final RestClient restClient;

    public MockWorkerClient(
        RestClient.Builder restClientBuilder,
        @Value("${external.mock-worker.base-url}") String baseUrl,
        @Value("${external.mock-worker.api-key}") String apiKey
    ) {
        this.restClient = restClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader("X-API-KEY", apiKey)
            .build();
    }

    public void processImage(String idempotencyKey) {
        restClient.post()
            .uri("/process")
            .body(new ProcessRequest(idempotencyKey))
            .retrieve()
            .toBodilessEntity();
    }
}
