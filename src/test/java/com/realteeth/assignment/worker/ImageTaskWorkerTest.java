package com.realteeth.assignment.worker;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.realteeth.assignment.global.exception.BusinessException;
import com.realteeth.assignment.global.exception.ErrorCode;
import com.realteeth.assignment.service.ImageTaskService;
import com.realteeth.assignment.worker.dto.response.ProcessStartResponse;
import java.time.Duration;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(value = DisplayNameGenerator.ReplaceUnderscores.class)
class ImageTaskWorkerTest {

    @Mock
    private ImageTaskService imageTaskService;

    @Mock
    private MockWorkerClient mockWorkerClient;

    @Mock
    private Acknowledgment acknowledgment;

    @Spy
    private RetryTemplate externalApiRetryTemplate = new RetryTemplate(
        RetryPolicy.builder()
            .maxRetries(2)
            .delay(Duration.ofMillis(1L))
            .multiplier(2.0)
            .maxDelay(Duration.ofSeconds(10))
            .build()
    );

    @InjectMocks
    private ImageTaskWorker imageTaskWorker;

    @Test
    void 이미지_처리_등록이_성공하면_ExternalJobId를_업데이트하고_커밋한다() {
        // given
        String message = "1";
        Long taskId = 1L;
        String imageUrl = "http://example.com/test.jpg";
        String jobId = "mock-job-123";
        ProcessStartResponse mockResponse = new ProcessStartResponse(jobId, "PROCESSING");

        when(imageTaskService.markAsProcessing(taskId)).thenReturn(imageUrl);
        when(mockWorkerClient.processImage(imageUrl)).thenReturn(mockResponse);

        // when
        imageTaskWorker.processTask(message, acknowledgment);

        // then
        verify(mockWorkerClient, times(1)).processImage(imageUrl);
        verify(imageTaskService, times(1)).updateExternalJobId(taskId, jobId);
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void 이미_처리중이거나_완료된_작업이면_외부API를_호출하지_않고_커밋만_한다() {
        // given
        String message = "2";
        Long taskId = 2L;

        when(imageTaskService.markAsProcessing(taskId))
            .thenThrow(new BusinessException(ErrorCode.INVALID_TASK_STATUS));

        // when
        imageTaskWorker.processTask(message, acknowledgment);

        // then
        verify(mockWorkerClient, never()).processImage(anyString());
        verify(imageTaskService, never()).updateExternalJobId(anyLong(), anyString());
        verify(imageTaskService, never()).markAsFailed(anyLong(), anyString());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void 외부_API_호출이_2번_실패후_3번째에_성공하면_ExternalJobId를_업데이트한다() {
        // given
        String message = "4";
        Long taskId = 4L;
        String imageUrl = "http://example.com/test.jpg";
        String jobId = "mock-job-456";
        ProcessStartResponse mockResponse = new ProcessStartResponse(jobId, "PROCESSING");

        when(imageTaskService.markAsProcessing(taskId)).thenReturn(imageUrl);

        when(mockWorkerClient.processImage(imageUrl))
            .thenThrow(new RuntimeException("API 1차 실패"))
            .thenThrow(new RuntimeException("API 2차 실패"))
            .thenReturn(mockResponse);

        // when
        imageTaskWorker.processTask(message, acknowledgment);

        // then
        verify(mockWorkerClient, times(3)).processImage(imageUrl);
        verify(imageTaskService, times(1)).updateExternalJobId(taskId, jobId);
        verify(imageTaskService, never()).markAsFailed(anyLong(), anyString());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void 외부_API_호출이_3번_모두_실패하면_FAILED로_변경하고_커밋한다() {
        // given
        String message = "3";
        Long taskId = 3L;
        String imageUrl = "http://example.com/test.jpg";

        when(imageTaskService.markAsProcessing(taskId)).thenReturn(imageUrl);
        when(mockWorkerClient.processImage(imageUrl)).thenThrow(new RuntimeException("API Timeout"));

        // when
        imageTaskWorker.processTask(message, acknowledgment);

        // then
        verify(mockWorkerClient, times(3)).processImage(imageUrl);
        verify(imageTaskService, never()).updateExternalJobId(anyLong(), anyString());
        verify(imageTaskService, times(1)).markAsFailed(eq(taskId), anyString());
        verify(acknowledgment, times(1)).acknowledge();
    }
}
