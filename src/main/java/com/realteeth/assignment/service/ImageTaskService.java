package com.realteeth.assignment.service;

import com.realteeth.assignment.domain.ImageTask;
import com.realteeth.assignment.global.exception.BusinessException;
import com.realteeth.assignment.global.exception.ErrorCode;
import com.realteeth.assignment.repository.ImageTaskRepository;
import com.realteeth.assignment.worker.MockWorkerClient;
import com.realteeth.assignment.worker.dto.response.TaskResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageTaskService {

    private static final String TOPIC = "image-process-topic";

    private final ImageTaskRepository imageTaskRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MockWorkerClient mockWorkerClient;

    public ImageTask createAndSaveTask(String idempotencyKey, String imageUrl) {
        try {
            ImageTask task = ImageTask.builder()
                .idempotencyKey(idempotencyKey)
                .imageUrl(imageUrl)
                .build();

            return imageTaskRepository.save(task);
        } catch (DataIntegrityViolationException e) {
            return imageTaskRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        }
    }

    @Transactional(readOnly = true)
    public ImageTask getTask(Long taskId) {
        return imageTaskRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }

    @Transactional
    public ImageTask markAsProcessing(Long taskId) {
        ImageTask task = getTask(taskId);

        task.startProcessing();
        return task;
    }

    @Transactional
    public ImageTask updateExternalJobId(Long taskId, String externalJobId) {
        ImageTask task = getTask(taskId);

        task.updateExternalJobId(externalJobId);
        log.info("Task ID: {} 에 External Job ID: {} 매핑 완료", taskId, externalJobId);
        return task;
    }

    @Transactional
    public ImageTask markAsCompleted(Long taskId, String resultData) {
        ImageTask task = getTask(taskId);

        task.complete(resultData);
        return task;
    }

    @Transactional
    public ImageTask markAsFailed(Long taskId, String errorMessage) {
        ImageTask task = getTask(taskId);

        task.fail(errorMessage);
        return task;
    }

    @Transactional(readOnly = true)
    public Page<TaskResultResponse> getTasks(Pageable pageable) {
        return imageTaskRepository.findAll(pageable)
            .map(TaskResultResponse::from);
    }
}
