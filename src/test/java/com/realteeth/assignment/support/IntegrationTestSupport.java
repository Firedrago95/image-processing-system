package com.realteeth.assignment.support;

import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public class IntegrationTestSupport {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL;

    @ServiceConnection
    static final KafkaContainer KAFKA;

    static {
        MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

        KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

        Startables.deepStart(MYSQL, KAFKA).join();
    }
}
