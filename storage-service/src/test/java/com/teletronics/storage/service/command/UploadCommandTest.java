package com.teletronics.storage.service.command;

import com.teletronics.storage.domain.UploadCommand;
import com.teletronics.storage.domain.Visibility;
import com.teletronics.storage.exception.InvalidUploadException;
import org.junit.jupiter.api.DisplayName;
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

@DisplayName("Upload command validation")
public class UploadCommandTest {

    @Test
    @DisplayName("Tags are trimmed")
    void tagsAreTrimmed() {
        UploadCommand command = commandWithTags(List.of(" work "));

        assertThat(command.tags()).containsExactly("work");
    }

    @Test
    @DisplayName("Tags are converted to lower case")
    void tagsAreLowerCased() {
        UploadCommand command = commandWithTags(List.of("WORK"));

        assertThat(command.tags()).containsExactly("work");
    }

    @Test
    @DisplayName("Duplicate tags collapse after normalization")
    void duplicateTagsCollapseAfterNormalization() {
        UploadCommand command = commandWithTags(List.of("Work", "WORK", " work "));

        assertThat(command.tags()).containsExactly("work");
    }

    @Test
    @DisplayName("Null tag list gives an empty set")
    void nullTagListProducesEmptySet() {
        UploadCommand command = commandWithTags(null);

        assertThat(command.tags()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Empty tag list gives an empty set")
    void emptyTagListProducesEmptySet() {
        UploadCommand command = commandWithTags(List.of());

        assertThat(command.tags()).isEmpty();
    }

    @Test
    @DisplayName("Exactly 5 unique tags are accepted")
    void exactlyFiveUniqueTagsAccepted() {
        UploadCommand command = commandWithTags(List.of("a", "b", "c", "d", "e"));

        assertThat(command.tags()).hasSize(5);
    }

    @Test
    @DisplayName("6 unique tags are rejected")
    void sixUniqueTagsRejected() {
        List<String> tags = List.of("a", "b", "c", "d", "e", "f");

        assertThatThrownBy(() -> commandWithTags(tags))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("at most 5");
    }

    @Test
    @DisplayName("The tag limit counts unique tags: 7 raw tags that collapse to 5 are accepted")
    void sevenRawTagsCollapsingToFiveUniqueAreAccepted_limitCountsUniqueTags() {
        UploadCommand command = commandWithTags(List.of("a", "A", "b", "B", "c", "d", "e"));

        assertThat(command.tags()).containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    }

    @ParameterizedTest
    @DisplayName("Blank tag is rejected: \"{0}\"")
    @ValueSource(strings = {"   ", ""})
    void blankTagsRejected(String tag) {
        List<String> tags = List.of(tag);

        assertThatThrownBy(() -> commandWithTags(tags))
                .isInstanceOf(InvalidUploadException.class);
    }

    @ParameterizedTest
    @DisplayName("Null or blank filename is rejected: {0}")
    @NullSource
    @ValueSource(strings = {"", "   "})
    void invalidFilenameRejected(String filename) {
        assertThatThrownBy(() -> new UploadCommand(
                "owner", filename, Visibility.PRIVATE, List.of(), null))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("filename must not be blank");
    }

    @Test
    @DisplayName("Missing visibility is rejected")
    void nullVisibilityRejected() {
        assertThatThrownBy(() -> new UploadCommand("owner", "file.txt", null, List.of(), null))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("visibility is required");
    }

    @Test
    @DisplayName("Null content type is accepted and kept")
    void nullContentTypeAcceptedAndPreserved() {
        UploadCommand command = commandWithContentType(null);

        assertThat(command.contentType()).isNull();
    }

    @Test
    @DisplayName("Valid content type is accepted")
    void validContentTypeAccepted() {
        UploadCommand command = commandWithContentType("application/pdf");

        assertThat(command.contentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Content type with a complex subtype is accepted")
    void complexSubtypeContentTypeAccepted() {
        String contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        UploadCommand command = commandWithContentType(contentType);

        assertThat(command.contentType()).isEqualTo(contentType);
    }

    @ParameterizedTest
    @DisplayName("Invalid content type is rejected: {0}")
    @ValueSource(strings = {"abc", "application/", "/pdf"})
    void invalidContentTypeRejected(String contentType) {
        assertThatThrownBy(() -> commandWithContentType(contentType))
                .isInstanceOf(InvalidUploadException.class);
    }

    @Test
    @DisplayName("Tag set cannot be modified")
    void tagSetIsUnmodifiable() {
        UploadCommand command = commandWithTags(List.of("work"));
        Set<String> tags = command.tags();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> tags.add("x"));
    }

    @Test
    @DisplayName("Canonical constructor copies the tag set, so later changes to the original do not leak in")
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
