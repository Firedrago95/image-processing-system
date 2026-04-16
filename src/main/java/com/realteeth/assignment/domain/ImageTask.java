package com.realteeth.assignment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "image_tasks")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ImageTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    private String resultData;

    @Version
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    private ImageTask(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        this.status = TaskStatus.PENDING;
    }

    public void startProcessing() throws IllegalAccessException {
        if (this.status != TaskStatus.PENDING) {
            throw new IllegalAccessException("PENDING 상태에서만 PROCESSING 으로 전환할 수 있습니다.");
        }
        this.status = TaskStatus.PROCESSING;
    }

    public void complete(String resultData) throws IllegalAccessException {
        if (this.status != TaskStatus.PROCESSING) {
            throw new IllegalAccessException("PROCESSING 상태에서만 COMPLETED로 전환할 수 있습니다.");
        }
        this.status = TaskStatus.COMPLETED;
        this.resultData = resultData;
    }

    public void fail(String errorMessage) throws IllegalAccessException {
        if (this.status == TaskStatus.COMPLETED) {
            throw new IllegalAccessException("이미 완료된 작업은 실패 처리할 수 없습니다.");
        }
        this.status = TaskStatus.FAILED;
        this.resultData = errorMessage;
    }
}
