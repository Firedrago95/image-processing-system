package com.realteeth.assignment.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 작업을 찾을 수 없습니다."),
    INVALID_TASK_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 작업 상태 전환입니다."),
    DUPLICATE_IDEMPOTENCY_KEY(HttpStatus.CONFLICT, "이미 처리 중이거나 완료된 요청입니다."),

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
