package com.realteeth.assignment.domain;

import com.realteeth.assignment.global.exception.BusinessException;
import com.realteeth.assignment.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(nullable = false)
    private String imageUrl;

    @Column(name = "external_job_id")
    private String externalJobId;

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
    private ImageTask(String idempotencyKey, String imageUrl) {
        this.idempotencyKey = idempotencyKey;
        this.imageUrl = imageUrl;
        this.status = TaskStatus.PENDING;
    }

    public void updateExternalJobId(String externalJobId) {
        this.externalJobId = externalJobId;
    }

    public void startProcessing() {
        if (this.status != TaskStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_TASK_STATUS);
        }
        this.status = TaskStatus.PROCESSING;
    }

    public void complete(String resultData) {
        if (this.status != TaskStatus.PROCESSING) {
            throw new BusinessException(ErrorCode.INVALID_TASK_STATUS);
        }
        this.status = TaskStatus.COMPLETED;
        this.resultData = resultData;
    }

    public void fail(String errorMessage) {
        if (this.status == TaskStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_TASK_STATUS);
        }
        this.status = TaskStatus.FAILED;
        this.resultData = errorMessage;
    }
}
