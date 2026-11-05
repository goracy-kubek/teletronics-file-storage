package com.teletronics.storage.domain;

import com.teletronics.storage.exception.InvalidUploadException;

import java.util.*;
import java.util.regex.Pattern;

import static com.teletronics.storage.utils.TagUtils.normalize;

/**
 * Input of the upload use case. Always valid: invalid input fails at construction.
 * Tags are normalized (trimmed, lower case, without duplicates).
 *
 * @param ownerId     id of the user who uploads the file
 * @param filename    filename to store; must not be blank
 * @param visibility  whether the file will appear in the public list; required
 * @param tags        normalized tags, at most 5 unique ones, none blank; empty if {@code null} is passed
 * @param contentType MIME type provided by the client, or {@code null} to detect it from the content
 * @throws InvalidUploadException if the filename is blank, the visibility is missing, a tag is blank,
 *                                there are more than 5 unique tags or the content type is not a valid MIME type
 */
public record UploadCommand(
        String ownerId,
        String filename,
        Visibility visibility,
        Set<String> tags,
        String contentType
) {
    private static final int MAX_TAGS = 5;
    private static final Pattern MIME_TYPE = Pattern.compile("^[\\w.+-]+/[\\w.+-]+$");

    /**
     * Creates a command from tags as supplied by the client: they are normalized
     * (trimmed, lower case, without duplicates) before validation.
     *
     * @param rawTags tags as supplied by the client, or {@code null}
     * @throws InvalidUploadException if the input is invalid, see the record description
     */
    public UploadCommand(
            String ownerId,
            String filename,
            Visibility visibility,
            List<String> rawTags,
            String contentType
    ) {
        this(ownerId, filename, visibility, normalize(rawTags), contentType);
    }

    public UploadCommand {
        tags = tags == null ? Set.of() : Set.copyOf(tags);

        require(filename != null && !filename.isBlank(), "filename must not be blank");
        require(visibility != null, "visibility is required");
        require(!tags.contains(""), "tags must not be blank");
        require(tags.size() <= MAX_TAGS, "at most " + MAX_TAGS + " unique tags are allowed");
        require(contentType == null || MIME_TYPE.matcher(contentType).matches(),
                "contentType is not a valid MIME type");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidUploadException(message);
        }
    }
}