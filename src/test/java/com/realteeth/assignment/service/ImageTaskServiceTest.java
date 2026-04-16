package com.realteeth.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.realteeth.assignment.domain.ImageTask;
import com.realteeth.assignment.repository.ImageTaskRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ImageTaskServiceTest {

    @Mock
    private ImageTaskRepository imageTaskRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private ImageTaskService imageTaskService;

    @Test
    void 최초_요청_시_DB에_저장되고_Kafka로_메시지가_발행된다() {
        // given
        String idempotencyKey = "new-key-123";
        ImageTask task = ImageTask.builder().idempotencyKey(idempotencyKey).build();
        ReflectionTestUtils.setField(task, "id", 1L);

        when(imageTaskRepository.save(any(ImageTask.class))).thenReturn(task);

        // when
        Long resultId = imageTaskService.processImage(idempotencyKey);

        // then
        assertThat(resultId).isEqualTo(1L);
        verify(imageTaskRepository, times(1)).save(any(ImageTask.class));
        verify(kafkaTemplate, times(1)).send(eq("image-process-topic"), eq("1"));
    }

    @Test
    void 중복_요청시_기존_작업_ID를_반환한다() {
        // given
        String idempotencyKey = "duplicate-key-123";
        ImageTask existingTask = ImageTask.builder().idempotencyKey(idempotencyKey).build();
        ReflectionTestUtils.setField(existingTask, "id", 2L);

        when(imageTaskRepository.save(any(ImageTask.class))).thenThrow(DataIntegrityViolationException.class);
        when(imageTaskRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingTask));

        // when
        Long resultId = imageTaskService.processImage(idempotencyKey);

        // then
        assertThat(resultId).isEqualTo(2L);
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }
}
