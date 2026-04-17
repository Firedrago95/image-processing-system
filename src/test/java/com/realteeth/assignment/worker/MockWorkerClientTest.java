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
        String idempotencyKey = "test-idempotency-key";
        String expectedBody = """
            {
                "idempotencyKey": "%s"
            }
            """.formatted(idempotencyKey);

        mockServer.expect(requestTo("http://dev.realteeth.ai/mock/process"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("X-API-KEY", "test-api-key-123"))
            .andExpect(content().json(expectedBody))
            .andRespond(withSuccess());

        // when & then
        assertThatCode(() -> mockWorkerClient.processImage(idempotencyKey))
            .doesNotThrowAnyException();

        mockServer.verify();
    }
}
