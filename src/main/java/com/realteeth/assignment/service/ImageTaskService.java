package com.realteeth.assignment.service;

import com.realteeth.assignment.domain.ImageTask;
import com.realteeth.assignment.global.exception.BusinessException;
import com.realteeth.assignment.global.exception.ErrorCode;
import com.realteeth.assignment.repository.ImageTaskRepository;
import com.realteeth.assignment.worker.dto.response.TaskResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImageTaskService {

    private static final String TOPIC = "image-process-topic";

    private final ImageTaskRepository imageTaskRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public Long processImage(String idempotencyKey) {
        try {
            ImageTask newTask = ImageTask.builder()
                .idempotencyKey(idempotencyKey)
                .build();

            ImageTask savedTask = imageTaskRepository.save(newTask);

            kafkaTemplate.send(TOPIC, savedTask.getId().toString());

            return savedTask.getId();
        } catch (DataIntegrityViolationException e) {
            return imageTaskRepository.findByIdempotencyKey(idempotencyKey)
                .map(ImageTask::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        }
    }

    @Transactional
    public String markAsProcessing(Long taskId) {
        ImageTask task = imageTaskRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        task.startProcessing();
        return task.getIdempotencyKey();
    }

    @Transactional
    public void markAsCompleted(Long taskId) {
        ImageTask task = imageTaskRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        task.complete("success");
    }

    @Transactional
    public void markAsFailed(Long taskId, String errorMessage) {
        ImageTask task = imageTaskRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        task.fail(errorMessage);
    }


    @Transactional(readOnly = true)
    public TaskResultResponse getTaskResult(Long taskId) {
        ImageTask task = imageTaskRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        return TaskResultResponse.from(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskResultResponse> getTasks(Pageable pageable) {
        return imageTaskRepository.findAll(pageable)
            .map(TaskResultResponse::from);
    }
}
