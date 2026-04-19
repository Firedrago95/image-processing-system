package com.realteeth.assignment.worker;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(
    components = MockWorkerClient.class,
    properties = {
        "external.mock-worker.base-url=http://dev.realteeth.ai/mock",
        "external.mock-worker.api-key=test-api-key-123"
    }
)
@MockitoBean(types = JpaMetamodelMappingContext.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MockWorkerClientTest {

    @Autowired
    private MockWorkerClient mockWorkerClient;

    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    void 외부_Mock_API로_이미지_처리_요청을_정상적으로_전송한다() {
        // given
        String imageUrl = "http://test.jpg";
        String expectedRequestBody = """
            {
                "imageUrl": "%s"
            }
            """.formatted(imageUrl);
        String expectedResponseBody = """
            {
                "jobId": "mock-job-123",
                "status": "PROCESSING"
            }
            """;

        mockServer.expect(requestTo("http://dev.realteeth.ai/mock/process"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("X-API-KEY", "test-api-key-123"))
            .andExpect(content().json(expectedRequestBody))
            .andRespond(withSuccess().body(expectedResponseBody).headers(headers()));

        // when & then
        assertThatCode(() -> mockWorkerClient.processImage(imageUrl))
            .doesNotThrowAnyException();
    }

    @Test
    void 외부_Mock_API로_작업_상태를_정상적으로_조회한다() {
        String jobId = "mock-job-123";
        String expectedResponseBody = """
            {
                "jobId": "mock-job-123",
                "status": "COMPLETED",
                "result": "success-url"
            }
            """;

        mockServer.expect(requestTo("http://dev.realteeth.ai/mock/process/" + jobId))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("X-API-KEY", "test-api-key-123"))
            .andRespond(withSuccess().body(expectedResponseBody).headers(headers()));

        // when & then
        assertThatCode(() -> mockWorkerClient.getJobStatus(jobId))
            .doesNotThrowAnyException();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        return headers;
    }
}
