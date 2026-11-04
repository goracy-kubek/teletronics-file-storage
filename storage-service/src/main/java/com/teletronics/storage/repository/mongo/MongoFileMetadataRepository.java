package com.teletronics.storage.repository.mongo;

import com.mongodb.DuplicateKeyException;
import com.teletronics.storage.domain.FileReservation;
import com.teletronics.storage.domain.StoredContent;
import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.mapper.FileDocumentMapper;
import com.teletronics.storage.exception.DuplicateFileException;
import com.teletronics.storage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

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
            log.error("Filename already exists: {}", reservation, e);
            throw new DuplicateFileException("filename already exists");
        }
    }

    @Override
    public StoredFile complete(String fileId, StoredContent content, String downloadToken) {
        return null;
    }

    @Override
    public void discard(String fileId) {
        throw new NotImplementedException();
    }
}
