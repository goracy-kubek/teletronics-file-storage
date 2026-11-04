package com.teletronics.storage.mapper;

import com.teletronics.storage.api.model.FileResponse;
import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.domain.Visibility;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

@Mapper
public interface FileResponseMapper {

    @Mapping(target = "downloadUrl", source = "downloadToken", qualifiedByName = "downloadUrl")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "sortedTags")
    FileResponse toResponse(StoredFile file);

    Visibility toDomain(com.teletronics.storage.api.model.Visibility visibility);

    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    @Named("downloadUrl")
    default String downloadUrl(String downloadToken) {
        return "/download/" + downloadToken;
    }

    @Named("sortedTags")
    default List<String> sortedTags(Set<String> tags) {
        return tags.stream().sorted().toList();
    }
}