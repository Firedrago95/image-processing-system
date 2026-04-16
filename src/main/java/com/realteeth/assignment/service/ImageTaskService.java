package com.realteeth.assignment.service;

import com.realteeth.assignment.domain.ImageTask;
import com.realteeth.assignment.global.exception.BusinessException;
import com.realteeth.assignment.global.exception.ErrorCode;
import com.realteeth.assignment.repository.ImageTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
}
