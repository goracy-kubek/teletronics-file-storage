package com.teletronics.storage.controller.handler;

import com.teletronics.storage.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Same answer for an unknown link, a missing file and another user's file. */
    @ExceptionHandler({DownloadLinkNotFoundException.class, OwnedFileNotFoundException.class})
    ProblemDetail notFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "File not found");
    }

    @ExceptionHandler(DuplicateFileException.class)
    ProblemDetail duplicate(DuplicateFileException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InvalidUploadException.class)
    ProblemDetail invalid(InvalidUploadException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(UploadInterruptedException.class)
    ProblemDetail interrupted(UploadInterruptedException e) {
        log.warn("Upload interrupted", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Upload interrupted");
    }

    @ExceptionHandler(ContentNotFoundException.class)
    ProblemDetail missingContent(ContentNotFoundException e) {
        log.error("Stored record without content", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");
    }
}