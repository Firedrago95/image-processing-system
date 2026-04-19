package com.realteeth.assignment.worker;

import com.realteeth.assignment.global.exception.BusinessException;
import com.realteeth.assignment.service.ImageTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageTaskWorker {

    private final ImageTaskService imageTaskService;
    private final MockWorkerClient mockWorkerClient;
    private final RetryTemplate externalApiRetryTemplate;

    @KafkaListener(topics = "image-process-topic", groupId = "image-worker-group")
    public void processTask(String message, Acknowledgment ack) {
        Long taskId = Long.valueOf(message);
        String idempotencyKey;

        try {
            idempotencyKey = imageTaskService.markAsProcessing(taskId);
        } catch (BusinessException e) {
            log.warn("작업을 시작할 수 없습니다. Task ID: {}, 원인: {}", taskId, e.getMessage());
            ack.acknowledge();
            return;
        }

        try {
            log.info("이미지 처리 API 호출 시작 Task ID: {}", taskId);

            externalApiRetryTemplate.execute(() -> {
                log.info("이미지 처리 API 호출 시도");
                mockWorkerClient.processImage(idempotencyKey);
                return null;
            });

            imageTaskService.markAsCompleted(taskId);
            log.info("이미지 처리 완료. Task ID: {}", taskId);
        } catch (Exception e) {
            log.error("이미지 처리 API 호출 실패 Task ID: {}", taskId, e);
            imageTaskService.markAsFailed(taskId, e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
}
