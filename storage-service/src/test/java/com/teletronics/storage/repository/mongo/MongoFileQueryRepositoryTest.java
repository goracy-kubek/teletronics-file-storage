package com.teletronics.storage.repository.mongo;

import com.teletronics.storage.TestcontainersConfiguration;
import com.teletronics.storage.config.mongo.MongoIndexInitializer;
import com.teletronics.storage.config.props.QueryProperties;
import com.teletronics.storage.mapper.FileDocumentMapperImpl;
import com.teletronics.storage.repository.FileMetadataRepository;
import com.teletronics.storage.repository.FileQueryRepository;
import com.teletronics.storage.repository.FileQueryRepositoryContractTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;

@DataMongoTest
@EnableConfigurationProperties(QueryProperties.class)
@Import({
        TestcontainersConfiguration.class,
        MongoFileQueryRepository.class,
        MongoFileMetadataRepository.class,
        FileDocumentMapperImpl.class,
        MongoIndexInitializer.class
})
class MongoFileQueryRepositoryTest extends FileQueryRepositoryContractTest {

    @Autowired
    private MongoFileQueryRepository queries;
    @Autowired
    private MongoFileMetadataRepository metadata;

    @Override
    protected FileQueryRepository queries() {
        return queries;
    }

    @Override
    protected FileMetadataRepository metadata() {
        return metadata;
    }
}