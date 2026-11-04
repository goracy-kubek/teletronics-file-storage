package com.teletronics.storage.repository.mongo;

import com.teletronics.storage.TestcontainersConfiguration;
import com.teletronics.storage.config.mongo.MongoIndexInitializer;
import com.teletronics.storage.domain.FileDocument;
import com.teletronics.storage.domain.FileReservation;
import com.teletronics.storage.domain.FileStatus;
import com.teletronics.storage.domain.StoredContent;
import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.domain.Visibility;
import com.teletronics.storage.mapper.FileDocumentMapperImpl;
import com.teletronics.storage.repository.FileMetadataRepository;
import com.teletronics.storage.repository.FileMetadataRepositoryContractTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

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

    @Override
    protected long countRecords(String ownerId) {
        return mongo.count(query(where("ownerId").is(ownerId)), FileDocument.class);
    }

    @Test
    void persistsReservedRecord() {
        StoredFile file = repository().reserve(
                new FileReservation("user-" + UUID.randomUUID(), "report.pdf", Visibility.PRIVATE, Set.of("work")));

        FileDocument stored = mongo.findById(file.id(), FileDocument.class);

        assertThat(stored).isNotNull();
        assertThat(stored.getOwnerId()).isEqualTo(file.ownerId());
        assertThat(stored.getFilename()).isEqualTo("report.pdf");
        assertThat(stored.getVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(stored.getTags()).containsExactly("work");
        assertThat(stored.getStatus()).isEqualTo(FileStatus.PENDING);
        // MongoDB stores dates with millisecond precision
        assertThat(stored.getUploadDate()).isEqualTo(file.uploadDate().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void persistsCompletedRecord() {
        StoredFile pending = repository().reserve(
                new FileReservation("user-" + UUID.randomUUID(), "report.pdf", Visibility.PRIVATE, Set.of()));
        repository().complete(pending.id(),
                new StoredContent("content-1", "sha-" + pending.id(), 1024, "application/pdf"),
                "token-" + pending.id());

        FileDocument stored = mongo.findById(pending.id(), FileDocument.class);

        assertThat(stored).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(FileStatus.READY);
        assertThat(stored.getContentId()).isEqualTo("content-1");
        assertThat(stored.getSha256()).isEqualTo("sha-" + pending.id());
        assertThat(stored.getSize()).isEqualTo(1024);
        assertThat(stored.getContentType()).isEqualTo("application/pdf");
        assertThat(stored.getDownloadToken()).isEqualTo("token-" + pending.id());
    }
}