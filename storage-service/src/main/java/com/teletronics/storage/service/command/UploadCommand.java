package com.teletronics.storage.service.command;

import com.teletronics.storage.domain.Visibility;
import com.teletronics.storage.exception.InvalidUploadException;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Input of the upload use case. Always valid: invalid input fails at construction.
 * Tags are normalized (trimmed, lower case, without duplicates).
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

    /**
     * Normalizes tags by trimming them, converting them to lower case, and removing duplicates.
     *
     * @param rawTags tags as supplied by the client, or {@code null}
     * @return an immutable set of normalized tags; empty if {@code rawTags} is {@code null}
     */
    private static Set<String> normalize(@Nullable List<String> rawTags) {
        return Stream.ofNullable(rawTags)
                .flatMap(List::stream)
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}