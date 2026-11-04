package com.teletronics.storage.repository.mongo;

import com.teletronics.storage.repository.FileContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Repository;
import com.mongodb.MongoGridFSException;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.InputStream;
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
}
