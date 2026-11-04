package com.teletronics.storage.service.command;

import com.teletronics.storage.domain.Visibility;
import com.teletronics.storage.exception.InvalidUploadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UploadCommandTest {

    @Test
    void tagsAreTrimmed() {
        UploadCommand command = commandWithTags(List.of(" work "));

        assertThat(command.tags()).containsExactly("work");
    }

    @Test
    void tagsAreLowerCased() {
        UploadCommand command = commandWithTags(List.of("WORK"));

        assertThat(command.tags()).containsExactly("work");
    }

    @Test
    void duplicateTagsCollapseAfterNormalization() {
        UploadCommand command = commandWithTags(List.of("Work", "WORK", " work "));

        assertThat(command.tags()).containsExactly("work");
    }

    @Test
    void nullTagListProducesEmptySet() {
        UploadCommand command = commandWithTags(null);

        assertThat(command.tags()).isNotNull().isEmpty();
    }

    @Test
    void emptyTagListProducesEmptySet() {
        UploadCommand command = commandWithTags(List.of());

        assertThat(command.tags()).isEmpty();
    }

    @Test
    void exactlyFiveUniqueTagsAccepted() {
        UploadCommand command = commandWithTags(List.of("a", "b", "c", "d", "e"));

        assertThat(command.tags()).hasSize(5);
    }

    @Test
    void sixUniqueTagsRejected() {
        List<String> tags = List.of("a", "b", "c", "d", "e", "f");

        assertThatThrownBy(() -> commandWithTags(tags))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("at most 5");
    }

    @Test
    void sevenRawTagsCollapsingToFiveUniqueAreAccepted_limitCountsUniqueTags() {
        UploadCommand command = commandWithTags(List.of("a", "A", "b", "B", "c", "d", "e"));

        assertThat(command.tags()).containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", ""})
    void blankTagsRejected(String tag) {
        List<String> tags = List.of(tag);

        assertThatThrownBy(() -> commandWithTags(tags))
                .isInstanceOf(InvalidUploadException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void invalidFilenameRejected(String filename) {
        assertThatThrownBy(() -> new UploadCommand(
                "owner", filename, Visibility.PRIVATE, List.of(), null))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("filename must not be blank");
    }

    @Test
    void nullVisibilityRejected() {
        assertThatThrownBy(() -> new UploadCommand("owner", "file.txt", null, List.of(), null))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("visibility is required");
    }

    @Test
    void nullContentTypeAcceptedAndPreserved() {
        UploadCommand command = commandWithContentType(null);

        assertThat(command.contentType()).isNull();
    }

    @Test
    void validContentTypeAccepted() {
        UploadCommand command = commandWithContentType("application/pdf");

        assertThat(command.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void complexSubtypeContentTypeAccepted() {
        String contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        UploadCommand command = commandWithContentType(contentType);

        assertThat(command.contentType()).isEqualTo(contentType);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "application/", "/pdf"})
    void invalidContentTypeRejected(String contentType) {
        assertThatThrownBy(() -> commandWithContentType(contentType))
                .isInstanceOf(InvalidUploadException.class);
    }

    @Test
    void tagSetIsUnmodifiable() {
        UploadCommand command = commandWithTags(List.of("work"));
        Set<String> tags = command.tags();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> tags.add("x"));
    }

    @Test
    void canonicalConstructorCopiesMutableSet() {
        Set<String> mutableTags = new HashSet<>();
        mutableTags.add("work");

        UploadCommand command = new UploadCommand(
                "owner", "file.txt", Visibility.PRIVATE, mutableTags, null);

        mutableTags.add("other");

        assertThat(command.tags()).containsExactly("work");
    }

    private static UploadCommand commandWithTags(List<String> tags) {
        return new UploadCommand("owner", "file.txt", Visibility.PRIVATE, tags, null);
    }

    private static UploadCommand commandWithContentType(String contentType) {
        return new UploadCommand("owner", "file.txt", Visibility.PRIVATE, List.of(), contentType);
    }
}
