package com.realteeth.assignment.worker.dto.response;

import com.realteeth.assignment.domain.ImageTask;
import com.realteeth.assignment.domain.TaskStatus;

public record TaskResultResponse(
    Long taskId,
    TaskStatus status,
    String resultData
) {
    public static TaskResultResponse from(ImageTask imageTask) {
        return new TaskResultResponse(
            imageTask.getId(),
            imageTask.getStatus(),
            imageTask.getResultData()
        );
    }
}
