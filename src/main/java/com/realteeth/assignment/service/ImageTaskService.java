package com.realteeth.assignment.service;

import com.realteeth.assignment.domain.ImageTask;
import com.realteeth.assignment.domain.TaskStatus;
import com.realteeth.assignment.global.exception.BusinessException;
import com.realteeth.assignment.global.exception.ErrorCode;
import com.realteeth.assignment.repository.ImageTaskRepository;
import com.realteeth.assignment.worker.MockWorkerClient;
import com.realteeth.assignment.worker.dto.response.ProcessStatusResponse;
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

    public Long processImage(String idempotencyKey, String imageUrl) {
        try {
            ImageTask newTask = ImageTask.builder()
                .idempotencyKey(idempotencyKey)
                .imageUrl(imageUrl)
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
        return task.getImageUrl();
    }

    // 🚀 2. 워커가 외부 서버 작업 등록 후 jobId를 매핑할 때 사용할 메서드
    @Transactional
    public void updateExternalJobId(Long taskId, String externalJobId) {
        ImageTask task = imageTaskRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        task.updateExternalJobId(externalJobId);
        log.info("Task ID: {} 에 External Job ID: {} 매핑 완료", taskId, externalJobId);
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

    @Transactional
    public TaskResultResponse getTaskResult(Long taskId) {
        ImageTask task = imageTaskRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        if (task.getStatus() == TaskStatus.PROCESSING && task.getExternalJobId() != null) {
            try {
                ProcessStatusResponse extStatus = mockWorkerClient.getJobStatus(task.getExternalJobId());

                if ("COMPLETED".equals(extStatus.status())) {
                    task.complete(extStatus.result());
                    log.info("Task ID: {} 외부 작업 완료 감지 및 동기화 성공", taskId);
                } else if ("FAILED".equals(extStatus.status())) {
                    task.fail(extStatus.result() != null ? extStatus.result() : "외부 작업 실패");
                    log.warn("Task ID: {} 외부 작업 실패 감지 및 동기화", taskId);
                }
            } catch (Exception e) {
                log.error("Task ID: {} 외부 상태 확인 중 에러 발생 (Job ID: {})", taskId, task.getExternalJobId(), e);
            }
        }

        return TaskResultResponse.from(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskResultResponse> getTasks(Pageable pageable) {
        return imageTaskRepository.findAll(pageable)
            .map(TaskResultResponse::from);
    }
}
