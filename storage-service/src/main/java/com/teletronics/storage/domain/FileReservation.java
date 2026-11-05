package com.teletronics.storage.domain;

import java.util.Set;

/**
 * Data needed to reserve a filename before the content is uploaded.
 * Becomes a {@link FileStatus#PENDING} record that holds the filename for the owner.
 *
 * @param ownerId    id of the user who uploads the file
 * @param filename   filename to reserve, unique per owner
 * @param visibility whether the file will appear in the public list
 * @param tags       normalized tags; empty if {@code null} is passed
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