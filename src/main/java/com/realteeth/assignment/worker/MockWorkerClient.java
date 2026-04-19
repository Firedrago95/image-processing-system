package com.realteeth.assignment.worker;

import com.realteeth.assignment.worker.dto.request.ProcessRequest;
import com.realteeth.assignment.worker.dto.response.ProcessStartResponse;
import com.realteeth.assignment.worker.dto.response.ProcessStatusResponse;
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

    public ProcessStartResponse processImage(String imageUrl) {
        return restClient.post()
            .uri("/process")
            .body(new ProcessRequest(imageUrl))
            .retrieve()
            .body(ProcessStartResponse.class);
    }

    public ProcessStatusResponse getJobStatus(String jobId) {
        return restClient.get()
            .uri("/process/{job_id}", jobId)
            .retrieve()
            .body(ProcessStatusResponse.class);
    }
}
