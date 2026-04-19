package com.realteeth.assignment.controller;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.realteeth.assignment.controller.dto.request.TaskRequest;
import com.realteeth.assignment.domain.ImageTask;
import com.realteeth.assignment.repository.ImageTaskRepository;
import com.realteeth.assignment.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ExtendWith(RestDocumentationExtension.class)
class ImageTaskControllerTest extends IntegrationTestSupport {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ImageTaskRepository imageTaskRepository;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(documentationConfiguration(restDocumentation))
            .build();
    }

    @Test
    void 이미지_처리_요청_시_202_상태코드와_taskId를_반환한다() throws Exception {
        // given
        String idempotencyKey = "docs-integration-key-123";
        TaskRequest request = new TaskRequest("http://example.com/test.jpg");

        // when & then
        mockMvc.perform(post("/v1/images/tasks")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.taskId").exists())
            .andDo(document("process-image",
                requestHeaders(
                    headerWithName("Idempotency-Key").description("중복 요청 방지를 위한 멱등성 키")
                ),
                requestFields(
                    fieldWithPath("imageUrl").description("처리할 원본 이미지 URL")
                ),
                responseFields(
                    fieldWithPath("taskId").description("발급된 내부 작업 식별자 ID")
                )
            ));
    }

    @Test
    void 작업_상태_조회_API_호출_시_200_상태코드와_결과를_반환한다() throws Exception {
        // given
        ImageTask task = ImageTask.builder().idempotencyKey("test-inquiry-key").imageUrl("http://test.jpg").build();
        ImageTask savedTask = imageTaskRepository.save(task);

        // when & then
        mockMvc.perform(get("/v1/images/tasks/{taskId}", savedTask.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").value(savedTask.getId()))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andDo(document("get-task-status",
                pathParameters(
                    parameterWithName("taskId").description("조회할 작업의 식별자 ID")
                ),
                responseFields(
                    fieldWithPath("taskId").description("작업 식별자 ID"),
                    fieldWithPath("status").description("현재 진행 상태 (PENDING, PROCESSING, COMPLETED, FAILED)"),
                    fieldWithPath("resultData").description("작업 결과 데이터 (완료 또는 실패 시 존재)").optional()
                )
            ));
    }

    @Test
    void 작업_목록_조회_API_호출_시_페이징된_결과를_반환한다() throws Exception {
        // given
        ImageTask task1 = ImageTask.builder().idempotencyKey("list-key-1").imageUrl("http://test.jpg1").build();
        ImageTask task2 = ImageTask.builder().idempotencyKey("list-key-2").imageUrl("http://test.jpg2").build();
        imageTaskRepository.saveAll(List.of(task1, task2));

        // when & then
        mockMvc.perform(get("/v1/images/tasks")
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andDo(document("get-tasks-list",
                queryParameters(
                    parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                    parameterWithName("size").description("페이지 크기").optional()
                ),
                responseFields(
                    fieldWithPath("content[].taskId").description("작업 식별자 ID"),
                    fieldWithPath("content[].status").description("작업 진행 상태"),
                    fieldWithPath("content[].resultData").description("작업 결과 데이터").optional(),
                    fieldWithPath("totalElements").description("전체 요소 수"),
                    fieldWithPath("totalPages").description("전체 페이지 수"),
                    fieldWithPath("size").description("페이지 크기"),
                    fieldWithPath("number").description("현재 페이지 번호"),
                    fieldWithPath("last").description("마지막 페이지 여부"),
                    fieldWithPath("first").description("첫 페이지 여부"),
                    fieldWithPath("numberOfElements").description("현재 페이지의 요소 수"),
                    fieldWithPath("empty").description("빈 페이지 여부"),
                    subsectionWithPath("pageable").ignored(),
                    subsectionWithPath("sort").ignored()
                )
            ));
    }
}
