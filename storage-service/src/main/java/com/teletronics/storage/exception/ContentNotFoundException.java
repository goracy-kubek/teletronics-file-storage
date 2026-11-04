package com.teletronics.storage.exception;

public class ContentNotFoundException extends RuntimeException {
    public ContentNotFoundException(String contentId) {
        super("Content not found: " + contentId);
    }
}