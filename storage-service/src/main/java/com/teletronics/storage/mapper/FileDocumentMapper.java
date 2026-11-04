package com.teletronics.storage.mapper;

import com.teletronics.storage.domain.FileDocument;
import com.teletronics.storage.domain.FileReservation;
import com.teletronics.storage.domain.StoredFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface FileDocumentMapper {

    StoredFile toDomain(FileDocument document);

    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "uploadDate", expression = "java(java.time.Instant.now())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "contentType", ignore = true)
    @Mapping(target = "size", ignore = true)
    @Mapping(target = "sha256", ignore = true)
    @Mapping(target = "downloadToken", ignore = true)
    @Mapping(target = "contentId", ignore = true)
    FileDocument toDocument(FileReservation reservation);
}