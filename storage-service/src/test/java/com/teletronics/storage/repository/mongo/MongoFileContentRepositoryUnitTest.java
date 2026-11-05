package com.teletronics.storage.repository.mongo;

import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Mongo file content repository (unit)")
@ExtendWith(MockitoExtension.class)
class MongoFileContentRepositoryUnitTest {

    private static final String CONTENT_ID = "6650f1c2a9e4b3d2c1a0f9e8";

    @Mock
    private GridFsTemplate gridFsTemplate;

    @InjectMocks
    private MongoFileContentRepository repository;

    @Test
    @DisplayName("Download wraps an IOException of the resource in UncheckedIOException")
    @SuppressWarnings("resource")
    void downloadWrapsIOExceptionOfTheResourceInUncheckedIOException() throws IOException {
        GridFSFile file = mock(GridFSFile.class);
        GridFSFindIterable found = mock(GridFSFindIterable.class);
        GridFsResource resource = mock(GridFsResource.class);
        IOException original = new IOException("chunk unreadable");

        when(gridFsTemplate.find(any(Query.class))).thenReturn(found);
        when(found.first()).thenReturn(file);
        when(gridFsTemplate.getResource(file)).thenReturn(resource);
        when(resource.getInputStream()).thenThrow(original);

        assertThatThrownBy(() -> repository.download(CONTENT_ID))
                .isInstanceOf(UncheckedIOException.class)
                .hasCause(original);
    }
}