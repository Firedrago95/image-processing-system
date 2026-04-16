package com.realteeth.assignment.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.realteeth.assignment.domain.ImageTask;
import com.realteeth.assignment.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImageTaskRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private ImageTaskRepository imageTaskRepository;

    @Test
    void 멱등성_키가_동일한_요청이_들어오면_예외가_발생한다() {
        // given
        String idempotencyKey = "duplacate-key-123";
        ImageTask task1 = ImageTask.builder().idempotencyKey(idempotencyKey).build();
        ImageTask task2 = ImageTask.builder().idempotencyKey(idempotencyKey).build();

        imageTaskRepository.save(task1);

        // when & then
        assertThatThrownBy(() -> imageTaskRepository.save(task2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
