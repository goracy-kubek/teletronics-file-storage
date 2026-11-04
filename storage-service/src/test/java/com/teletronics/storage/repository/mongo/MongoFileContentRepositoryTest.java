package com.teletronics.storage.repository.mongo;

import com.teletronics.storage.TestcontainersConfiguration;
import com.teletronics.storage.repository.FileContentRepository;
import com.teletronics.storage.repository.FileContentRepositoryContractTest;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@DataMongoTest
@Import({TestcontainersConfiguration.class, MongoFileContentRepository.class})
class MongoFileContentRepositoryTest extends FileContentRepositoryContractTest {

    @Autowired
    private MongoTemplate mongo;
    @Autowired
    private MongoFileContentRepository repository;
    @Autowired
    private GridFsTemplate gridFs;

    @Override
    protected FileContentRepository repository() {
        return repository;
    }

    @Override
    protected byte[] read(String contentId) {
        try (var in = gridFs.getResource(gridFs.findOne(query(where("_id").is(new ObjectId(contentId))))).getInputStream()) {
            return in.readAllBytes();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @Override
    protected boolean exists(String contentId) {
        return mongo.exists(query(where("_id").is(new ObjectId(contentId))), "fs.files");
    }

    @Test
    void deleteRemovesAllChunks() throws Exception {
        String id = repository().store(new ByteArrayInputStream(new byte[1024 * 1024]));
        Query chunksOfFile = query(where("files_id").is(new ObjectId(id)));
        assertThat(mongo.count(chunksOfFile, "fs.chunks")).isPositive();

        repository().delete(id);

        assertThat(mongo.count(chunksOfFile, "fs.chunks")).isZero();
    }
}