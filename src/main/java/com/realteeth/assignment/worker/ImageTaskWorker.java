package com.realteeth.assignment.worker;

import com.realteeth.assignment.global.exception.BusinessException;
import com.realteeth.assignment.service.ImageTaskService;
import com.realteeth.assignment.worker.dto.response.ProcessStartResponse;
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

        try {
            String imageUrl = imageTaskService.markAsProcessing(taskId);
            log.info("이미지 처리 API 호출 시작 Task ID: {}", taskId);

            ProcessStartResponse startResponse = externalApiRetryTemplate.execute(() ->
                mockWorkerClient.processImage(imageUrl)
            );

            imageTaskService.updateExternalJobId(taskId, startResponse.jobId());

            log.info("외부 작업 등록 완료. Task ID: {}, Job ID: {}", taskId, startResponse.jobId());
        } catch (BusinessException e) {
            log.warn("작업을 시작할 수 없습니다. Task ID: {}, 원인: {}", taskId, e.getMessage());
        } catch (Exception e) {
            log.error("이미지 처리 통신 에러 Task ID: {}", taskId, e);
            imageTaskService.markAsFailed(taskId, "외부 API 연동 실패");
        } finally {
            ack.acknowledge(); // 🚀 즉시 커밋하여 스레드 반환
        }
    }
}
