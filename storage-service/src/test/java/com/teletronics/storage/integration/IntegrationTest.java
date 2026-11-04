package com.teletronics.storage.integration;

import com.teletronics.storage.TestcontainersConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
abstract class IntegrationTest {

    @LocalServerPort
    int port;

    final HttpClient http = HttpClient.newHttpClient();

    URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}