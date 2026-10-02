package mega.privacy.android.domain.entity.zipbrowser

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZipArchiveEntryTest {

    @Test
    fun `test that isNestedArchive is true for a zip entry regardless of case`() {
        assertThat(entry("a/inner.ZIP").isNestedArchive).isTrue()
    }

    @Test
    fun `test that isNestedArchive is false for a folder whose name ends in zip`() {
        assertThat(entry("a/looks.zip", isDirectory = true).isNestedArchive).isFalse()
    }

    @Test
    fun `test that isNestedArchive is false for a file of another type`() {
        assertThat(entry("a/photo.jpg").isNestedArchive).isFalse()
    }

    private fun entry(path: String, isDirectory: Boolean = false) = ZipArchiveEntry(
        index = 0,
        path = path,
        name = path.substringAfterLast('/'),
        parentPath = path.substringBeforeLast('/', "").takeIf { it.isNotEmpty() },
        size = 1L,
        modificationTime = 0L,
        isDirectory = isDirectory,
        isEncrypted = false,
        isNameDecodable = true,
    )
}
