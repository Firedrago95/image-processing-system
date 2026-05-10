package com.realteeth.assignment.service;

import com.realteeth.assignment.domain.ImageTask;
import com.realteeth.assignment.domain.TaskStatus;
import com.realteeth.assignment.worker.MockWorkerClient;
import com.realteeth.assignment.worker.dto.response.ProcessStatusResponse;
import com.realteeth.assignment.worker.dto.response.TaskResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageTaskApplicationService {

    private static final String TOPIC = "image-process-topic";

    private final ImageTaskService imageTaskService;
    private final MockWorkerClient mockWorkerClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public Long requestImageProcessing(String idempotencyKey, String imageUrl) {
        ImageTask imageTask = imageTaskService.createAndSaveTask(idempotencyKey, imageUrl);

        if (imageTask.getStatus() == TaskStatus.PENDING) {
            kafkaTemplate.send(TOPIC, imageTask.getId().toString());
        }

        return imageTask.getId();
    }

    public TaskResultResponse getTaskResult(Long taskId) {
        ImageTask imageTask = imageTaskService.getTask(taskId);

        updateProcessStatus(taskId, imageTask);
        return TaskResultResponse.from(imageTask);
    }

    public Page<TaskResultResponse> getTasks(Pageable pageable) {
        return imageTaskService.getTasks(pageable);
    }

    private void updateProcessStatus(Long taskId, ImageTask imageTask) {
        if (imageTask.isProcessing()) {
            try {
                ProcessStatusResponse extStatus = mockWorkerClient.getJobStatus(imageTask.getExternalJobId());

                if ("COMPLETED".equals(extStatus.status())) {
                    imageTaskService.markAsCompleted(taskId, extStatus.result());
                    imageTask.complete(extStatus.result());
                    log.info("Task ID: {} 완료 동기화", taskId);
                } else if ("FAILED".equals(extStatus.status())) {
                    String error = extStatus.result() != null ? extStatus.result() : "외부 작업 실패";
                    imageTaskService.markAsFailed(taskId, error);
                    imageTask.fail(error);
                    log.warn("Task ID: {} 실패 동기화", taskId);
                }
            } catch (Exception e) {
                log.error("Task ID: {} 동기화 에러", taskId, e);
            }
        }
    }
}
