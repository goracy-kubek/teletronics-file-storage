package com.teletronics.storage.repository.mongo;

import com.teletronics.storage.repository.FileContentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;

@Repository
@RequiredArgsConstructor
public class MongoFileContentRepository implements FileContentRepository {
    @Override
    public String store(InputStream content) throws IOException {
        return "";
    }

    @Override
    public void delete(String contentId) {
        throw new NotImplementedException();
    }
}
