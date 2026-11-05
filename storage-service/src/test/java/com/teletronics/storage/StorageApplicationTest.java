package com.teletronics.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Storage application")
class StorageApplicationTest {

    @Test
    @DisplayName("Application starts through its main method")
    void mainStartsTheApplication() {
        ConfigurableApplicationContext context = SpringApplication
                .from(StorageApplication::main)
                .with(TestcontainersConfiguration.class)
                .run("--server.port=0")
                .getApplicationContext();

        try (context) {
            assertThat(context.isRunning()).isTrue();
        }
    }
}
