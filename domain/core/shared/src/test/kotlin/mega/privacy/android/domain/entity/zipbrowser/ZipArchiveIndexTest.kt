package mega.privacy.android.domain.entity.zipbrowser

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZipArchiveIndexTest {

    @Test
    fun `test that childrenOf returns the root entries when the parent path is null`() {
        val underTest = index(
            entry(0, "a", isDirectory = true),
            entry(1, "a/b.txt"),
            entry(2, "c.txt"),
        )

        assertThat(underTest.childrenOf(null).map { it.path })
            .containsExactly("a", "c.txt").inOrder()
    }

    @Test
    fun `test that childrenOf returns only the direct children of a folder`() {
        val underTest = index(
            entry(0, "a", isDirectory = true),
            entry(1, "a/b.txt"),
            entry(2, "a/c", isDirectory = true),
            entry(3, "a/c/d.txt"),
        )

        assertThat(underTest.childrenOf("a").map { it.path })
            .containsExactly("a/b.txt", "a/c").inOrder()
    }

    @Test
    fun `test that childrenOf returns an empty list for a folder with no children`() {
        val underTest = index(entry(0, "empty", isDirectory = true))

        assertThat(underTest.childrenOf("empty")).isEmpty()
    }

    @Test
    fun `test that childrenOf preserves central directory order`() {
        val underTest = index(
            entry(2, "z.txt"),
            entry(0, "a.txt"),
            entry(1, "m.txt"),
        )

        assertThat(underTest.childrenOf(null).map { it.path })
            .containsExactly("a.txt", "m.txt", "z.txt").inOrder()
    }

    @Test
    fun `test that fileCountOf counts files recursively and ignores folders`() {
        val underTest = index(
            entry(0, "a", isDirectory = true),
            entry(1, "a/b.txt"),
            entry(2, "a/c", isDirectory = true),
            entry(3, "a/c/d.txt"),
        )

        assertThat(underTest.fileCountOf("a")).isEqualTo(2)
    }

    @Test
    fun `test that folderCountOf counts only the direct child folders`() {
        val underTest = index(
            entry(0, "a", isDirectory = true),
            entry(1, "a/b", isDirectory = true),
            entry(2, "a/c", isDirectory = true),
            entry(3, "a/b/d", isDirectory = true),
            entry(4, "a/e.txt"),
        )

        assertThat(underTest.folderCountOf("a")).isEqualTo(2)
    }

    private fun entry(
        index: Int,
        path: String,
        isDirectory: Boolean = false,
        size: Long = 1L,
    ) = ZipArchiveEntry(
        index = index,
        path = path,
        name = path.substringAfterLast('/'),
        parentPath = path.substringBeforeLast('/', "").takeIf { it.isNotEmpty() },
        size = size,
        modificationTime = 0L,
        isDirectory = isDirectory,
        isEncrypted = false,
        isNameDecodable = true,
    )

    private fun index(vararg entries: ZipArchiveEntry) = ZipArchiveIndex(
        archivePath = "/tmp/a.zip",
        entries = entries.associateBy { it.path },
        isEncrypted = false,
    )
}
