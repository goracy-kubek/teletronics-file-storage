package com.teletronics.storage.repository;

import com.teletronics.storage.domain.FileReservation;
import com.teletronics.storage.domain.StoredContent;
import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.domain.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.teletronics.storage.testutils.ConcurrentUtils.awaitClockAfter;
import static org.assertj.core.api.Assertions.*;

public abstract class FileQueryRepositoryContractTest {

    private static final String TEXT = "text/plain";

    protected abstract FileQueryRepository queries();

    /** Used only to create test data, since the query repository is read-only. */
    protected abstract FileMetadataRepository metadata();

    private String owner;
    /**
     * Unique per test. Tests that read the public list add it to their files and filter by it,
     * so they see only their own data. Own-list tests don't need it: the unique owner isolates them.
     */
    private String tag;

    @BeforeEach
    void setUp() {
        owner = "user-" + UUID.randomUUID();
        tag = "tag-" + UUID.randomUUID();
    }

    @Test
    void findsReadyFileByToken() {
        StoredFile file = create(owner, "report.pdf", Visibility.PRIVATE, 10, TEXT);

        Optional<StoredFile> found = queries().findReadyByToken(file.downloadToken());

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(file.id());
    }

    @Test
    void unknownTokenGivesEmpty() {
        assertThat(queries().findReadyByToken("no-such-token-" + owner)).isEmpty();
    }

    @Test
    void ownListHasBothVisibilitiesAndOnlyOwnerFiles() {
        create(owner, "public.txt", Visibility.PUBLIC, 10, TEXT);
        create(owner, "private.txt", Visibility.PRIVATE, 10, TEXT);
        create("user-" + UUID.randomUUID(), "foreign.txt", Visibility.PUBLIC, 10, TEXT);

        Page<StoredFile> page = queries().findOwn(owner, null, PageRequest.of(0, 10));

        assertThat(names(page)).containsExactlyInAnyOrder("public.txt", "private.txt");
    }

    @Test
    void uploadsInProgressAreInvisible() {
        metadata().reserve(new FileReservation(owner, "pending.txt", Visibility.PUBLIC, Set.of()));
        create(owner, "ready.txt", Visibility.PUBLIC, 10, TEXT);

        Page<StoredFile> page = queries().findOwn(owner, null, PageRequest.of(0, 10));

        assertThat(names(page)).containsExactly("ready.txt");
    }

    @Test
    void tagFilterIsExactAndCaseInsensitive() {
        String work = tag + "-work";
        create(owner, "tagged.txt", Visibility.PUBLIC, 10, TEXT, work);
        create(owner, "plain.txt", Visibility.PUBLIC, 10, TEXT);

        var page = PageRequest.of(0, 10);
        Page<StoredFile> exact = queries().findOwn(owner, work.toUpperCase(), page);

        var partOfTag = work.substring(0, work.length() - 2);
        Page<StoredFile> partial = queries().findOwn(owner, partOfTag, page);

        assertThat(names(exact)).containsExactly("tagged.txt");
        assertThat(partial.getContent()).isEmpty();
    }

    @Test
    void unknownTagGivesEmptyPage() {
        create(owner, "file.txt", Visibility.PUBLIC, 10, TEXT);

        Page<StoredFile> page = queries().findOwn(owner, "unknown-" + tag, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void pagesDoNotOverlapEvenWhenSortValuesAreEqual() {
        List<String> created = IntStream.range(0, 7)
                .mapToObj(i -> "file-" + i + ".txt")
                .toList();
        created.forEach(name -> create(owner, name, Visibility.PUBLIC, 10, TEXT));

        List<String> listed = IntStream.range(0, 3)
                .mapToObj(p -> queries().findOwn(owner, null, PageRequest.of(p, 2, Sort.by("size"))))
                .flatMap(page -> page.getContent().stream())
                .map(StoredFile::filename)
                .toList();

        assertThat(listed)
                .hasSize(6)
                .doesNotHaveDuplicates()
                .isSubsetOf(created);
    }

    @Test
    void sortsBySize() {
        create(owner, "medium.txt", Visibility.PUBLIC, 20, TEXT);
        create(owner, "small.txt", Visibility.PUBLIC, 10, TEXT);
        create(owner, "large.txt", Visibility.PUBLIC, 30, TEXT);

        assertThat(names(ownSorted(Sort.by("size"))))
                .containsExactly("small.txt", "medium.txt", "large.txt");
        assertThat(names(ownSorted(Sort.by("size").descending())))
                .containsExactly("large.txt", "medium.txt", "small.txt");
    }

    @Test
    void sortsByFilename() {
        create(owner, "banana.txt", Visibility.PUBLIC, 1, TEXT);
        create(owner, "apple.txt", Visibility.PUBLIC, 1, TEXT);
        create(owner, "cherry.txt", Visibility.PUBLIC, 1, TEXT);

        assertThat(names(ownSorted(Sort.by("filename"))))
                .containsExactly("apple.txt", "banana.txt", "cherry.txt");
        assertThat(names(ownSorted(Sort.by("filename").descending())))
                .containsExactly("cherry.txt", "banana.txt", "apple.txt");
    }

    @Test
    void filenameSortIsCaseSensitive() {
        create(owner, "apple.txt", Visibility.PUBLIC, 1, TEXT);
        create(owner, "Zebra.txt", Visibility.PUBLIC, 1, TEXT);

        assertThat(names(ownSorted(Sort.by("filename")))).containsExactly("Zebra.txt", "apple.txt");
    }

    @Test
    void sortsByContentType() {
        create(owner, "text.txt", Visibility.PUBLIC, 1, TEXT);
        create(owner, "doc.pdf", Visibility.PUBLIC, 1, "application/pdf");
        create(owner, "pic.png", Visibility.PUBLIC, 1, "image/png");

        assertThat(names(ownSorted(Sort.by("contentType"))))
                .containsExactly("doc.pdf", "pic.png", "text.txt");
        assertThat(names(ownSorted(Sort.by("contentType").descending())))
                .containsExactly("text.txt", "pic.png", "doc.pdf");
    }

    @Test
    void sortsByTags() {
        create(owner, "b.txt", Visibility.PUBLIC, 1, TEXT, "b");
        create(owner, "az.txt", Visibility.PUBLIC, 1, TEXT, "a", "z");
        create(owner, "c.txt", Visibility.PUBLIC, 1, TEXT, "c");

        assertThat(names(ownSorted(Sort.by("tags"))))
                .containsExactly("az.txt", "b.txt", "c.txt");
        assertThat(names(ownSorted(Sort.by("tags").descending())))
                .containsExactly("az.txt", "c.txt", "b.txt");
    }

    @Test
    void sortsByUploadDate() {
        StoredFile first = create(owner, "first.txt", Visibility.PUBLIC, 1, TEXT);
        awaitClockAfter(first.uploadDate());
        StoredFile second = create(owner, "second.txt", Visibility.PUBLIC, 1, TEXT);
        awaitClockAfter(second.uploadDate());
        create(owner, "third.txt", Visibility.PUBLIC, 1, TEXT);

        assertThat(names(ownSorted(Sort.by("uploadDate"))))
                .containsExactly("first.txt", "second.txt", "third.txt");
        assertThat(names(ownSorted(Sort.by("uploadDate").descending())))
                .containsExactly("third.txt", "second.txt", "first.txt");
    }

    @Test
    void sortsByTwoFields() {
        create(owner, "b.txt", Visibility.PUBLIC, 20, TEXT);
        create(owner, "a.txt", Visibility.PUBLIC, 10, TEXT);
        create(owner, "c.pdf", Visibility.PUBLIC, 30, "application/pdf");

        Sort sort = Sort.by("contentType").and(Sort.by("size").descending());

        assertThat(names(ownSorted(sort))).containsExactly("c.pdf", "b.txt", "a.txt");
    }

    @Test
    void sortIsAppliedBeforePaging() {
        IntStream.rangeClosed(1, 5).forEach(i ->
                create(owner, "s" + i * 10 + ".txt", Visibility.PUBLIC, i * 10L, TEXT));

        Page<StoredFile> secondPage = queries().findOwn(owner, null, PageRequest.of(1, 2, Sort.by("size")));

        assertThat(names(secondPage)).containsExactly("s30.txt", "s40.txt");
    }

    @Test
    void rejectsSortByNonWhitelistedProperty() {
        FileQueryRepository repo = queries();
        Pageable bySecret = PageRequest.of(0, 10, Sort.by("ownerId"));

        assertThatThrownBy(() -> repo.findOwn(owner, null, bySecret))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot sort by ownerId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"filename", "uploadDate", "contentType", "size", "tags"})
    void acceptsEveryWhitelistedSortProperty(String property) {
        FileQueryRepository repo = queries();
        Pageable sorted = PageRequest.of(0, 10, Sort.by(property));

        assertThatCode(() -> repo.findOwn(owner, null, sorted)).doesNotThrowAnyException();
    }

    @Test
    void publicListHasPublicFilesOfAllUsersAndNeverPrivate() {
        create(owner, "mine-public.txt", Visibility.PUBLIC, 10, TEXT, tag);
        create(owner, "mine-private.txt", Visibility.PRIVATE, 10, TEXT, tag);
        create("user-" + UUID.randomUUID(), "their-public.txt", Visibility.PUBLIC, 10, TEXT, tag);
        create("user-" + UUID.randomUUID(), "their-private.txt", Visibility.PRIVATE, 10, TEXT, tag);

        Page<StoredFile> page = queries().findPublic(tag, PageRequest.of(0, 10));

        assertThat(names(page)).containsExactlyInAnyOrder("mine-public.txt", "their-public.txt");
    }

    @Test
    void publicListSupportsTagFilter() {
        String other = tag + "-other";
        create(owner, "tagged.txt", Visibility.PUBLIC, 10, TEXT, other);
        create(owner, "plain.txt", Visibility.PUBLIC, 10, TEXT, tag);

        Page<StoredFile> page = queries().findPublic(other, PageRequest.of(0, 10));

        assertThat(names(page)).containsExactly("tagged.txt");
    }

    @Test
    void publicListIsSortable() {
        create(owner, "large.txt", Visibility.PUBLIC, 20, TEXT, tag);
        create(owner, "small.txt", Visibility.PUBLIC, 10, TEXT, tag);

        Page<StoredFile> page = queries().findPublic(tag, PageRequest.of(0, 10, Sort.by("size")));

        assertThat(names(page)).containsExactly("small.txt", "large.txt");
    }

    @Test
    void publicListRejectsNonWhitelistedSort() {
        FileQueryRepository repo = queries();
        Pageable bySecret = PageRequest.of(0, 10, Sort.by("sha256"));

        assertThatThrownBy(() -> repo.findPublic(tag, bySecret))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot sort by sha256");
    }


    private static List<String> names(Page<StoredFile> page) {
        return page.getContent().stream().map(StoredFile::filename).toList();
    }

    private Page<StoredFile> ownSorted(Sort sort) {
        return queries().findOwn(owner, null, PageRequest.of(0, 10, sort));
    }

    private StoredFile create(
            String ownerId,
            String filename,
            Visibility visibility,
            long size,
            String contentType,
            String... tags
    ) {
        StoredFile pending = metadata().reserve(
                new FileReservation(ownerId, filename, visibility, Set.copyOf(Arrays.asList(tags))));
        String unique = UUID.randomUUID().toString();
        StoredContent content = new StoredContent("content-" + unique, "sha-" + unique, size, contentType);

        return metadata().complete(pending.id(), content, "token-" + unique);
    }
}