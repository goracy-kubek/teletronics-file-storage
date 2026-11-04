package com.teletronics.storage.repository.mongo;

import com.teletronics.storage.domain.FileDocument;
import com.teletronics.storage.domain.FileReservation;
import com.teletronics.storage.domain.FileStatus;
import com.teletronics.storage.domain.StoredContent;
import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.exception.DuplicateFileException;
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
        } catch (DuplicateKeyException e) {
            log.debug("Filename already exists: {}", reservation, e);
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
        } catch (DuplicateKeyException e) {
            log.debug("Same content already uploaded: fileId={}", fileId, e);
            throw new DuplicateFileException("same content already uploaded");
        }
    }

    private static Query pendingRecord(String fileId) {
        return query(where("_id").is(fileId).and("status").is(FileStatus.PENDING));
    }

    private static Update completion(StoredContent content, String downloadToken) {
        return new Update()
                .set("contentId", content.contentId())
                .set("sha256", content.sha256())
                .set("size", content.size())
                .set("contentType", content.contentType())
                .set("downloadToken", downloadToken)
                .set("status", FileStatus.READY);
    }

    @Override
    public void discard(String fileId) {
        mongoTemplate.remove(pendingRecord(fileId), FileDocument.class);
    }
}
