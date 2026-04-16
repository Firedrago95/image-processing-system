package com.realteeth.assignment.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.realteeth.assignment.support.IntegrationTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ImageTaskControllerTest extends IntegrationTestSupport {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void 이미지_처리_요청_시_202_상태코드와_taskId를_반환한다() throws Exception {
        // given
        String idempotencyKey = "real-integration-key-123";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // when
        ResponseEntity<Map> response = restTemplate.exchange(
            "/v1/images/tasks",
            HttpMethod.POST,
            requestEntity,
            Map.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("taskId")).isNotNull();
    }
}
