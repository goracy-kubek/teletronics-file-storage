package com.teletronics.storage.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

/**
 * File metadata as stored in MongoDB, collection {@code files}.
 * <p>
 * Persistence model only: the domain works with {@code StoredFile}, and
 * {@code FileDocumentMapper} converts between the two. File content is not stored
 * here but in GridFS, referenced by {@link #contentId}.
 * <p>
 * Uniqueness is enforced by indexes created in {@code MongoIndexInitializer}:
 * {ownerId, filename}, {ownerId, sha256} and {downloadToken}.
 */
@Document("files")
@Getter
@Setter
@NoArgsConstructor
public class FileDocument {

    /** Generated id of the file record. */
    @Id
    String id;

    /** Id of the user who uploaded the file, taken from the X-User-Id header. */
    String ownerId;

    /** Filename, unique per owner. Can be changed without re-uploading the content. */
    String filename;

    /** Whether the file appears in the public list of all users. */
    Visibility visibility;

    /** Up to 5 tags, normalized to lower case. */
    Set<String> tags;

    /** MIME type provided by the user or detected from the content after upload. */
    String contentType;

    /** Content size in bytes. */
    long size;

    /**
     * SHA-256 of the content, hex-encoded. Unique per owner.
     * Absent while the upload is in progress ({@link FileStatus#PENDING}).
     */
    String sha256;

    /** Time when the upload was started. */
    Instant uploadDate;

    /**
     * Random, non-guessable token of the download link {@code /download/{downloadToken}}.
     * Absent while the upload is in progress.
     */
    String downloadToken;

    /** Id of the content in GridFS. Absent while the upload is in progress. */
    String contentId;

    /** Upload state: PENDING while content is being uploaded, READY when the file is available. */
    FileStatus status;
}