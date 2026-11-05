package com.teletronics.storage.utils;

import com.teletronics.storage.domain.FileDocument;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * Helpers for building paging parameters from API request values.
 */
@UtilityClass
public class PageUtils {
    /** API sort names that differ from the stored field name. */
    private final Map<String, String> API_TO_FIELD = Map.of("tag", FileDocument.Fields.tags);

    /**
     * Builds a {@link Pageable} from the {@code sort} query parameter and the page numbers.
     * The sort has the form {@code property[,direction]}, for example {@code uploadDate,desc}.
     * The direction is case-insensitive and defaults to ascending. API names such as {@code tag}
     * are mapped to the stored field name; other names are used as they are.
     * Whether the property may be sorted by is not checked here.
     *
     * @param sort sort expression, {@code property} or {@code property,direction}
     * @param page zero-based page number
     * @param size page size
     * @return the pageable with a single sort order
     * @throws IllegalArgumentException if the direction is neither {@code asc} nor {@code desc}
     */
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
