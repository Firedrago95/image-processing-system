package com.realteeth.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.realteeth.assignment.domain.ImageTask;
import com.realteeth.assignment.domain.TaskStatus;
import com.realteeth.assignment.global.exception.BusinessException;
import com.realteeth.assignment.global.exception.ErrorCode;
import com.realteeth.assignment.worker.MockWorkerClient;
import com.realteeth.assignment.worker.dto.response.ProcessStatusResponse;
import com.realteeth.assignment.worker.dto.response.TaskResultResponse;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayNameGeneration(value = DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ImageTaskFacadeTest {

    @Mock
    private ImageTaskService imageTaskService;
    @Mock
    private MockWorkerClient mockWorkerClient;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private ImageTaskFacade imageTaskFacade;

    @Test
    void 요청_시_저장하고_PENDING_상태인_경우에만_Kafka_메시지를_보낸다() {
        // given
        ImageTask task = ImageTask.builder().idempotencyKey("key").build();
        ReflectionTestUtils.setField(task, "id", 100L);
        when(imageTaskService.createAndSaveTask(anyString(), anyString())).thenReturn(task);

        // when
        Long id = imageTaskFacade.requestImageProcessing("key", "url");

        // then
        assertThat(id).isEqualTo(100L);
        verify(kafkaTemplate).send(eq("image-process-topic"), eq("100"));
    }

    @Test
    void 결과_조회_시_PROCESSING_상태라면_외부_API를_호출하여_동기화한다() {
        // given
        Long taskId = 1L;
        String jobId = "job-777";
        String expectedResult = "success-result-data"; // 테스트용 결과 데이터

        ImageTask task = ImageTask.builder().idempotencyKey("key").build();
        task.startProcessing();
        task.updateExternalJobId(jobId);

        when(imageTaskService.getTask(taskId)).thenReturn(task);

        when(mockWorkerClient.getJobStatus(jobId))
            .thenReturn(new ProcessStatusResponse(jobId, "COMPLETED", expectedResult));

        // when
        TaskResultResponse response = imageTaskFacade.getTaskResult(taskId);

        // then
        verify(mockWorkerClient).getJobStatus(jobId);
        verify(imageTaskService).markAsCompleted(taskId, expectedResult);

        assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.resultData()).isEqualTo(expectedResult);
    }

    @Test
    void 외부_API_에러_발생_시에도_사용자_조회는_성공해야_한다() {
        // given
        Long taskId = 1L;
        ImageTask task = ImageTask.builder().idempotencyKey("key").build();
        task.startProcessing();
        task.updateExternalJobId("job-id");

        when(imageTaskService.getTask(taskId)).thenReturn(task);
        when(mockWorkerClient.getJobStatus(anyString())).thenThrow(new RuntimeException("API Fail"));

        // when
        TaskResultResponse response = imageTaskFacade.getTaskResult(taskId);

        // then
        assertThat(response.status()).isEqualTo(TaskStatus.PROCESSING);
        verify(imageTaskService, never()).markAsCompleted(any(), anyString());
    }

    @Test
    void 외부_작업_상태가_FAILED라면_서비스의_실패_처리_메서드를_호출한다() {
        // given
        Long taskId = 1L;
        String jobId = "ext-job-fail";
        String errorMsg = "이미지 처리 중 메모리 부족";

        ImageTask task = ImageTask.builder().idempotencyKey("fail-sync").build();
        task.startProcessing();
        task.updateExternalJobId(jobId);

        when(imageTaskService.getTask(taskId)).thenReturn(task);
        when(mockWorkerClient.getJobStatus(jobId))
            .thenReturn(new ProcessStatusResponse(jobId, "FAILED", errorMsg));

        // when
        TaskResultResponse response = imageTaskFacade.getTaskResult(taskId);

        // then
        verify(imageTaskService).markAsFailed(taskId, errorMsg);
        assertThat(response.status()).isEqualTo(TaskStatus.FAILED);
        assertThat(response.resultData()).isEqualTo(errorMsg);
    }

    @Test
    void 존재하지_않는_작업_조회_시_서비스의_예외가_그대로_전파된다() {
        // given
        Long invalidTaskId = 999L;
        when(imageTaskService.getTask(invalidTaskId))
            .thenThrow(new BusinessException(ErrorCode.TASK_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> imageTaskFacade.getTaskResult(invalidTaskId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TASK_NOT_FOUND);
    }
}
