package com.realteeth.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.realteeth.assignment.domain.ImageTask;
import com.realteeth.assignment.domain.TaskStatus;
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

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ImageTaskServiceTest {

    @Mock
    private ImageTaskRepository imageTaskRepository;

    @InjectMocks
    private ImageTaskService imageTaskService;

    @Test
    void 신규_태스크_생성_시_DB에_저장한다() {
        // given
        String idempotencyKey = "new-key";
        ImageTask task = ImageTask.builder().idempotencyKey(idempotencyKey).build();
        when(imageTaskRepository.save(any(ImageTask.class))).thenReturn(task);

        // when
        ImageTask result = imageTaskService.createAndSaveTask(idempotencyKey, "http://image.jpg");

        // then
        assertThat(result.getIdempotencyKey()).isEqualTo(idempotencyKey);
        verify(imageTaskRepository).save(any(ImageTask.class));
    }

    @Test
    void 멱등성_키_충돌_시_기존_데이터를_조회하여_반환한다() {
        // given
        String key = "dup-key";
        ImageTask existingTask = ImageTask.builder().idempotencyKey(key).build();
        when(imageTaskRepository.save(any(ImageTask.class))).thenThrow(DataIntegrityViolationException.class);
        when(imageTaskRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingTask));

        // when
        ImageTask result = imageTaskService.createAndSaveTask(key, "url");

        // then
        assertThat(result).isEqualTo(existingTask);
        verify(imageTaskRepository).findByIdempotencyKey(key);
    }

    @Test
    void 상태_변경_메서드들은_엔티티의_비즈니스_로직을_수행한다() {
        // given
        Long taskId = 1L;
        ImageTask task = ImageTask.builder().idempotencyKey("key").build();
        when(imageTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        // when
        imageTaskService.markAsProcessing(taskId);

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PROCESSING);
    }
}
