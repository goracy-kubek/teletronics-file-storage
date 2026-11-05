package com.teletronics.storage.utils;

import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class TagUtils {

    /**
     * Normalizes one tag: trims it and converts it to lower case.
     *
     * @param tag a tag as supplied by the client
     * @return the normalized tag
     */
    public static String normalize(String tag) {
        return tag.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes tags by trimming them, converting them to lower case, and removing duplicates.
     *
     * @param rawTags tags as supplied by the client, or {@code null}
     * @return an immutable set of normalized tags; empty if {@code rawTags} is {@code null}
     */
    public static Set<String> normalize(@Nullable List<String> rawTags) {
        return Stream.ofNullable(rawTags)
                .flatMap(List::stream)
                .map(TagUtils::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }
}