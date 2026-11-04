package com.teletronics.storage.utils;

import com.teletronics.storage.domain.FileDocument;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

@UtilityClass
public class PageUtils {
    private final Map<String, String> API_TO_FIELD = Map.of("tag", FileDocument.Fields.tags);

    public Pageable pageableOf(String sort, int page, int size) {
        String[] parts = sort.split(",", 2);
        String name = parts[0].trim();
        String property = API_TO_FIELD.getOrDefault(name, name);

        Sort.Direction direction = parts.length > 1
                ? Sort.Direction.fromString(parts[1].trim())
                : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
