package com.realteeth.assignment.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.realteeth.assignment.domain.ImageTask;
import com.realteeth.assignment.repository.ImageTaskRepository;
import com.realteeth.assignment.support.IntegrationTestSupport;
import java.util.List;
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

    @Autowired
    private ImageTaskRepository imageTaskRepository;

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

    @Test
    void 작업_상태_조회_API_호출_시_200_상태코드와_결과를_반환한다() {
        // given
        ImageTask task = ImageTask.builder().idempotencyKey("test-inquiry-key").build();
        ImageTask savedTask = imageTaskRepository.save(task);

        // when
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/v1/images/tasks/" + savedTask.getId(),
            Map.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(((Number) response.getBody().get("taskId")).longValue()).isEqualTo(savedTask.getId());
        assertThat(response.getBody().get("status")).isEqualTo("PENDING");
    }

    @Test
    void 작업_목록_조회_API_호출_시_페이징된_결과를_반환한다() {
        // given
        ImageTask task1 = ImageTask.builder().idempotencyKey("list-key-1").build();
        ImageTask task2 = ImageTask.builder().idempotencyKey("list-key-2").build();
        imageTaskRepository.saveAll(List.of(task1, task2));

        // when
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/v1/images/tasks?page=0&size=1",
            Map.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(1);
        assertThat(((Number) response.getBody().get("totalElements")).longValue()).isGreaterThanOrEqualTo(2L);
    }
}
