package com.realteeth.assignment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.realteeth.assignment.global.exception.BusinessException;
import com.realteeth.assignment.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImageTaskTest {

    @Test
    void 객체_생성_시_초기_상태는_PENDING이다() {
        // given
        String idempotencyKey = "test-key-123";

        // when
        ImageTask task = ImageTask.builder().idempotencyKey(idempotencyKey).build();

        // then
        assertThat(task.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void PENDING_상태에서_PROCESSING으로_상태를_변경할_수_있다() {
        // given
        ImageTask task = ImageTask.builder().idempotencyKey("test-key").build();

        // when
        task.startProcessing();

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PROCESSING);
    }

    @Test
    void PENDING이_아닌_상태에서_PROCESSING으로_변경하면_예외가_발생한다() {
        // given
        ImageTask task = ImageTask.builder().idempotencyKey("test-key").build();
        task.startProcessing();

        // when & then
        assertThatThrownBy(task::startProcessing)
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TASK_STATUS);
    }

    @Test
    void PROCESSING_상태에서_COMPLETED로_변경하고_결과를_저장할_수_있다() {
        // given
        ImageTask task = ImageTask.builder().idempotencyKey("test-key").build();
        task.startProcessing();
        String resultData = "이미지_처리_결과_URL";

        // when
        task.complete(resultData);

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getResultData()).isEqualTo(resultData);
    }

    @Test
    void PROCESSING이_아닌_상태에서_COMPLETED로_변경하면_예외가_발생한다() {
        // given
        ImageTask task = ImageTask.builder().idempotencyKey("test-key").build();

        // when & then
        assertThatThrownBy(() -> task.complete("결과 데이터"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TASK_STATUS);
    }

    @Test
    void 완료되지_않은_작업은_FAILED로_변경하고_에러메시지를_저장할_수_있다() {
        // given
        ImageTask task = ImageTask.builder().idempotencyKey("test-key").build();
        task.startProcessing();
        String errorMessage = "API Timeout Exception";

        // when
        task.fail(errorMessage);

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.getResultData()).isEqualTo(errorMessage);
    }

    @Test
    void 이미_COMPLETED_상태인_경우_FAILED로_변경하면_예외가_발생한다() {
        // given
        ImageTask task = ImageTask.builder().idempotencyKey("test-key").build();
        task.startProcessing();
        task.complete("성공");

        // when & then
        assertThatThrownBy(() -> task.fail("에러 발생"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TASK_STATUS);
    }
}
