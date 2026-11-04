package com.teletronics.storage.config.mongo;

import com.teletronics.storage.domain.FileDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.stereotype.Component;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Creates the unique indexes on the {@code files} collection that enforce the duplicate rules.
 * Runs once all beans are created, before the web server starts accepting requests.
 * Index creation is idempotent: existing indexes are left as they are.
 */
@Component
@RequiredArgsConstructor
public class MongoIndexInitializer implements SmartInitializingSingleton {

    private final MongoTemplate mongo;

    @Override
    public void afterSingletonsInstantiated() {
        createIndexes();
    }

    /**
     * Creates all indexes of the {@code files} collection.
     */
    public void createIndexes() {
        IndexOperations files = mongo.indexOps(FileDocument.class);
        createFilenameIndex(files);
        createContentIndex(files);
        createDownloadTokenIndex(files);
    }

    /**
     * One filename per owner ("Filename already exists").
     * Not partial: the filename is set from the start, so it is reserved already while PENDING.
     * Different owners may use the same filename.
     */
    private static void createFilenameIndex(IndexOperations files) {
        files.createIndex(new Index()
                .named("owner_filename")
                .on("ownerId", Sort.Direction.ASC)
                .on("filename", Sort.Direction.ASC)
                .unique());
    }

    /**
     * One content per owner
     * Partial: PENDING records have no hash yet and must not conflict with each other.
     */
    private static void createContentIndex(IndexOperations files) {
        files.createIndex(new Index()
                .named("owner_sha256")
                .on("ownerId", Sort.Direction.ASC)
                .on("sha256", Sort.Direction.ASC)
                .unique()
                .partial(PartialIndexFilter.of(where("sha256").exists(true))));
    }

    /**
     * Fast lookup of a file by its download link; a token never points to two files.
     * Unique across all owners. Partial: PENDING records have no token yet.
     */
    private static void createDownloadTokenIndex(IndexOperations files) {
        files.createIndex(new Index()
                .named("download_token")
                .on("downloadToken", Sort.Direction.ASC)
                .unique()
                .partial(PartialIndexFilter.of(where("downloadToken").exists(true))));
    }
}