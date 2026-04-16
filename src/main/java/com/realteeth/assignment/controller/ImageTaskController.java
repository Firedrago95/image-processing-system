package com.realteeth.assignment.controller;

import com.realteeth.assignment.controller.dto.response.TaskResponse;
import com.realteeth.assignment.service.ImageTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/images")
@RequiredArgsConstructor
public class ImageTaskController {

    private final ImageTaskService imageTaskService;

    @PostMapping("/tasks")
    public ResponseEntity<TaskResponse> processImage(
        @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        Long taskId = imageTaskService.processImage(idempotencyKey);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(new TaskResponse(taskId));
    }
}
