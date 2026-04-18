package com.realteeth.assignment.controller;

import com.realteeth.assignment.controller.dto.response.TaskResponse;
import com.realteeth.assignment.service.ImageTaskService;
import com.realteeth.assignment.worker.dto.response.TaskResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/images/tasks")
@RequiredArgsConstructor
public class ImageTaskController {

    private final ImageTaskService imageTaskService;

    @PostMapping
    public ResponseEntity<TaskResponse> processImage(
        @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        Long taskId = imageTaskService.processImage(idempotencyKey);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(new TaskResponse(taskId));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResultResponse> getTaskResult(@PathVariable Long taskId) {
        TaskResultResponse response = imageTaskService.getTaskResult(taskId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<TaskResultResponse>> getTasks(
        @PageableDefault(size = 10, sort="createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TaskResultResponse> responses = imageTaskService.getTasks(pageable);
        return ResponseEntity.ok(responses);
    }
}
