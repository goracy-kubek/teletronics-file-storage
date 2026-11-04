package com.teletronics.storage.repository.mongo;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.teletronics.storage.exception.ContentNotFoundException;
import com.teletronics.storage.repository.FileContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Repository;
import com.mongodb.MongoGridFSException;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Repository
@RequiredArgsConstructor
public class MongoFileContentRepository implements FileContentRepository {
    private final GridFsTemplate gridFsTemplate;

    @Override
    public String store(InputStream content) throws IOException {
        try {
            ObjectId id = gridFsTemplate.store(content, UUID.randomUUID().toString());
            return id.toHexString();
        } catch (MongoGridFSException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }

            throw e;
        }
    }

    @Override
    public void delete(String contentId) {
        if (ObjectId.isValid(contentId)) {
            gridFsTemplate.delete(query(where("_id").is(new ObjectId(contentId))));
        }
    }

    @Override
    public InputStream download(String contentId) {
        GridFSFile file = findFile(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        try {
            return gridFsTemplate.getResource(file).getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Optional<GridFSFile> findFile(String contentId) {
        return Optional.of(contentId)
                .filter(ObjectId::isValid)
                .map(ObjectId::new)
                .map(id -> gridFsTemplate.find(query(where("_id").is(id))).first());
    }
}
