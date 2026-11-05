package com.teletronics.storage.repository.mongo;

import com.teletronics.storage.domain.FileDocument;
import com.teletronics.storage.domain.FileDocument.Fields;
import com.teletronics.storage.domain.FileReservation;
import com.teletronics.storage.domain.FileStatus;
import com.teletronics.storage.domain.StoredContent;
import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.exception.DuplicateFileException;
import com.teletronics.storage.exception.OwnedFileNotFoundException;
import com.teletronics.storage.mapper.FileDocumentMapper;
import com.teletronics.storage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MongoFileMetadataRepository implements FileMetadataRepository {
    private final MongoTemplate mongoTemplate;
    private final FileDocumentMapper fileDocumentMapper;

    @Override
    public StoredFile reserve(FileReservation reservation) {
        try {
            return fileDocumentMapper.toDomain(mongoTemplate.insert(fileDocumentMapper.toDocument(reservation)));
        } catch (DuplicateKeyException _) {
            log.debug("Filename already exists: filename={}", reservation.filename());
            throw new DuplicateFileException("filename already exists");
        }
    }

    @Override
    public StoredFile complete(String fileId, StoredContent content, String downloadToken) {
        return Optional.ofNullable(findAndComplete(fileId, content, downloadToken))
                .map(fileDocumentMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("No PENDING record with id " + fileId));
    }

    private FileDocument findAndComplete(String fileId, StoredContent content, String downloadToken) {
        try {
            return mongoTemplate.findAndModify(
                    pendingRecord(fileId),
                    completion(content, downloadToken),
                    FindAndModifyOptions.options().returnNew(true),
                    FileDocument.class
            );
        } catch (DuplicateKeyException _) {
            log.debug("Same content already uploaded: fileId={}", fileId);
            throw new DuplicateFileException("same content already uploaded");
        }
    }

    private static Query pendingRecord(String fileId) {
        return query(where(Fields.id).is(fileId).and(Fields.status).is(FileStatus.PENDING));
    }

    private static Query readyRecordOf(String fileId, String ownerId) {
        return query(where(Fields.id).is(fileId)
                .and(Fields.ownerId).is(ownerId)
                .and(Fields.status).is(FileStatus.READY));
    }

    private static Update completion(StoredContent content, String downloadToken) {
        return new Update()
                .set(Fields.contentId, content.contentId())
                .set(Fields.sha256, content.sha256())
                .set(Fields.size, content.size())
                .set(Fields.contentType, content.contentType())
                .set(Fields.downloadToken, downloadToken)
                .set(Fields.status, FileStatus.READY);
    }

    @Override
    public void discard(String fileId) {
        mongoTemplate.remove(pendingRecord(fileId), FileDocument.class);
    }

    @Override
    public StoredFile deleteOwned(String fileId, String ownerId) {
        return Optional.ofNullable(mongoTemplate.findAndRemove(readyRecordOf(fileId, ownerId), FileDocument.class))
                .map(fileDocumentMapper::toDomain)
                .orElseThrow(() -> new OwnedFileNotFoundException(fileId, ownerId));
    }

    @Override
    public StoredFile rename(String fileId, String ownerId, String newFilename) {
        try {
            return Optional.ofNullable(findAndRename(fileId, ownerId, newFilename))
                    .map(fileDocumentMapper::toDomain)
                    .orElseThrow(() -> new OwnedFileNotFoundException(fileId, ownerId));
        } catch (DuplicateKeyException _) {
            log.debug("Filename already exists: fileId={}", fileId);
            throw new DuplicateFileException("filename already exists");
        }
    }

    private FileDocument findAndRename(String fileId, String ownerId, String newFilename) {
        return mongoTemplate.findAndModify(
                readyRecordOf(fileId, ownerId),
                new Update().set(Fields.filename, newFilename),
                FindAndModifyOptions.options().returnNew(true),
                FileDocument.class
        );
    }
}