package com.teletronics.storage.repository.mongo;

import com.teletronics.storage.config.props.QueryProperties;
import com.teletronics.storage.domain.FileDocument;
import com.teletronics.storage.domain.FileStatus;
import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.domain.Visibility;
import com.teletronics.storage.mapper.FileDocumentMapper;
import com.teletronics.storage.repository.FileQueryRepository;
import com.teletronics.storage.utils.TagUtils;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Repository
@RequiredArgsConstructor
public class MongoFileQueryRepository implements FileQueryRepository {
    private final QueryProperties properties;
    private final MongoTemplate mongoTemplate;
    private final FileDocumentMapper fileDocumentMapper;

    @Override
    public Optional<StoredFile> findReadyByToken(String downloadToken) {
        Criteria criteria = where("downloadToken")
                .is(downloadToken).and("status")
                .is(FileStatus.READY);

        return Optional.ofNullable(mongoTemplate.findOne(query(criteria), FileDocument.class))
                .map(fileDocumentMapper::toDomain);
    }

    @Override
    public Page<StoredFile> findOwn(String ownerId, @Nullable String tag, Pageable pageable) {
        return find(readyFiles().and("ownerId").is(ownerId), tag, pageable);
    }

    @Override
    public Page<StoredFile> findPublic(@Nullable String tag, Pageable pageable) {
        return find(readyFiles().and("visibility").is(Visibility.PUBLIC), tag, pageable);
    }

    private Page<StoredFile> find(Criteria base, @Nullable String tag, Pageable pageable) {
        Query filter = query(tag == null ? base : base.and("tags").is(TagUtils.normalize(tag)));
        Query pageQuery = Query.of(filter).with(withStableOrder(pageable));

        List<StoredFile> content = mongoTemplate.find(pageQuery, FileDocument.class).stream()
                .map(fileDocumentMapper::toDomain)
                .toList();

        return PageableExecutionUtils.getPage(content, pageable,
                () -> mongoTemplate.count(Query.of(filter).limit(-1).skip(-1), FileDocument.class));
    }

    private static Criteria readyFiles() {
        return where("status").is(FileStatus.READY);
    }

    private Pageable withStableOrder(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!properties.sortableProperties().contains(order.getProperty())) {
                throw new IllegalArgumentException("Cannot sort by " + order.getProperty());
            }
        });

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().and(Sort.by("id"))
        );
    }
}