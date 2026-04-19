package com.realteeth.assignment.worker.dto.response;

public record ProcessStatusResponse(
    String jobId,
    String status,
    String result
) {

}
