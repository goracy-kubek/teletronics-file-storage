package com.teletronics.storage.domain;

/**
 * Whether the file appears in the public list of all users.
 * It does not restrict downloads: anyone with the download link can fetch the file either way.
 */
public enum Visibility {
    /** Listed in the public list of all users, as well as in the owner's own list. */
    PUBLIC,
    /** Listed only in the owner's own list. */
    PRIVATE
}