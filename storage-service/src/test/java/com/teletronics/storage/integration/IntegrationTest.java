package com.teletronics.storage.integration;

import com.teletronics.storage.TestcontainersConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;

/**
 * Base class of the integration tests. Starts the whole application on a random port,
 * with MongoDB running in a Testcontainers container, and provides an HTTP client
 * for calling it over the network.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
abstract class IntegrationTest {

    @LocalServerPort
    int port;

    final HttpClient http = HttpClient.newHttpClient();

    /**
     * @param path path with an optional query string, starting with {@code /}
     * @return the full URL of the running application for this path
     */
    URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}