package com.teletronics.storage.repository.mongo;

import com.teletronics.storage.TestcontainersConfiguration;
import com.teletronics.storage.config.mongo.MongoIndexInitializer;
import com.teletronics.storage.mapper.FileDocumentMapperImpl;
import com.teletronics.storage.repository.FileMetadataRepository;
import com.teletronics.storage.repository.FileMetadataRepositoryContractTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

@DataMongoTest
@Import({
        TestcontainersConfiguration.class,
        MongoFileMetadataRepository.class,
        FileDocumentMapperImpl.class,
        MongoIndexInitializer.class
})
class MongoFileMetadataRepositoryTest extends FileMetadataRepositoryContractTest {

    @Autowired
    private MongoFileMetadataRepository repository;
    @Autowired
    private MongoTemplate mongo;
    @Autowired
    private MongoIndexInitializer indexes;

    @BeforeEach
    void createIndexes() {
        indexes.createIndexes();
    }

    @Override
    protected FileMetadataRepository repository() {
        return repository;
    }
}