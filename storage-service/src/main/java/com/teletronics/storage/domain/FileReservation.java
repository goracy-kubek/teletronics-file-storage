package com.teletronics.storage.domain;

import java.util.Set;

/**
 * Data needed to reserve a filename before the content is uploaded.
 */
public record FileReservation(
        String ownerId,
        String filename,
        Visibility visibility,
        Set<String> tags
) {
    public FileReservation {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }
}