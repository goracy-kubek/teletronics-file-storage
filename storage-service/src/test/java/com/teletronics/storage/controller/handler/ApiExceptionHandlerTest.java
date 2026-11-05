package com.teletronics.storage.controller.handler;

import com.teletronics.storage.exception.ContentNotFoundException;
import com.teletronics.storage.exception.InvalidUploadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("API exception handler")
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    @DisplayName("Stored record without content is an internal error that does not reveal the content id")
    void missingContentIsReportedAsInternalError() {
        ProblemDetail problem = handler.missingContent(new ContentNotFoundException("6650f1c2a9e4b3d2c1a0f9e8"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo("Internal error");
    }

    @Test
    @DisplayName("Invalid upload is a bad request with the reason as detail")
    void invalidUploadIsReportedAsBadRequestWithItsMessage() {
        ProblemDetail problem = handler.invalid(new InvalidUploadException("file must not be empty"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("file must not be empty");
    }
}
