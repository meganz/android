package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import nz.mega.sdk.MegaNode
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FileTypeInfoMapperTest {
    private val underTest = ::getFileTypeInfo

    @Test
    fun `test that a node with no extension returns UnMappedFileTypeInfo`() {
        val expectedMimeType = "a mime type"
        val node = mock<MegaNode> { on { name }.thenReturn("NoExtension.") }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            UnMappedFileTypeInfo(
                ""
            )
        )
    }

    @Test
    fun `test that extension is mapped if present`() {
        val expectedExtension = "txt"
        val node = mock<MegaNode> { on { name }.thenReturn("withExtension.$expectedExtension") }

        assertThat(underTest(node) { "type" }.extension).isEqualTo(expectedExtension)
    }

    @Test
    fun `test that a pdf is mapped correctly`() {
        val node = mock<MegaNode> { on { name }.thenReturn("withExtension.pdf") }

        assertThat(underTest(node) { "application/pdf" }).isEqualTo(
            PdfFileTypeInfo
        )
    }

    @Test
    fun `test that a zip file is mapped correctly`() {
        val expectedExtension = "zip"
        val expectedMimeType = "application/zip"
        val node = mock<MegaNode> { on { name }.thenReturn("withExtension.$expectedExtension") }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            ZipFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension
            )
        )
    }

    @Test
    fun `test that a multipart zip file is mapped correctly`() {
        val expectedExtension = "zip"
        val expectedMimeType = "multipart/x-zip"
        val node = mock<MegaNode> { on { name }.thenReturn("withExtension.$expectedExtension") }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            ZipFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension
            )
        )
    }

    @Test
    fun `test that a url is mapped correctly`() {
        val expectedExtension = "some web extension?"
        val expectedMimeType = "web/url "
        val node = mock<MegaNode> { on { name }.thenReturn("withExtension.$expectedExtension") }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            UrlFileTypeInfo
        )
    }

    @Test
    fun `test that an image file is mapped correctly`() {
        val expectedExtension = "jpg"
        val expectedMimeType = "image/jpeg"
        val node = mock<MegaNode> { on { name }.thenReturn("withExtension.$expectedExtension") }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            StaticImageFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension
            )
        )
    }

    @Test
    fun `test that a file with audio type is mapped correctly`() {
        val expectedExtension = "mp3"
        val expectedMimeType = "audio/"
        val expectedDuration = 120
        val node = mock<MegaNode> {
            on { name }.thenReturn("withExtension.$expectedExtension")
            on { duration }.thenReturn(expectedDuration)
        }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            AudioFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension,
                duration = expectedDuration
            )
        )
    }

    @Test
    fun `test that a file with opus extension is mapped correctly`() {
        val expectedExtension = "opus"
        val expectedMimeType = "opus type"
        val expectedDuration = 120
        val node = mock<MegaNode> {
            on { name }.thenReturn("withExtension.$expectedExtension")
            on { duration }.thenReturn(expectedDuration)
        }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            AudioFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension,
                duration = expectedDuration,
            )
        )
    }

    @Test
    fun `test that a file with weba extension is mapped correctly`() {
        val expectedExtension = "weba"
        val expectedMimeType = "weba type"
        val expectedDuration = 120
        val node = mock<MegaNode> {
            on { name }.thenReturn("withExtension.$expectedExtension")
            on { duration }.thenReturn(expectedDuration)
        }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            AudioFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension,
                duration = expectedDuration,
            )
        )
    }

    @Test
    fun `test that a file with gif extension is mapped correctly`() {
        val expectedExtension = "gif"
        val expectedMimeType = "image/gif"
        val node = mock<MegaNode> { on { name }.thenReturn("withExtension.$expectedExtension") }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            GifFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension
            )
        )
    }

    @Test
    fun `test that a file with capital gif  extension is mapped correctly`() {
        val expectedExtension = "GIF"
        val expectedMimeType = "image/gif"
        val node =
            mock<MegaNode> { on { name }.thenReturn("withExtension.$expectedExtension") }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            GifFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension
            )
        )
    }

    @Test
    fun `test that a file with raw extension is mapped correctly`() {
        val expectedMimeType = "expected"
        val node = mock<MegaNode>()
        listOf(
            //Raw
            "3fr", "arw", "cr2",
            "crw", "ciff", "cs1",
            "dcr", "dng", "erf",
            "iiq", "k25", "kdc",
            "mef", "mos", "mrw",
            "nef", "nrw", "orf",
            "pef", "raf", "raw",
            "rw2", "rwl", "sr2",
            "srf", "srw", "x3f",
        ).forEach {
            val expectedExtension = it
            whenever(node.name).thenReturn("withExtension.$expectedExtension")
            assertThat(underTest(node) { expectedMimeType }).isEqualTo(
                RawFileTypeInfo(
                    mimeType = expectedMimeType,
                    extension = expectedExtension
                )
            )
        }
    }

    @Test
    fun `test that a file with capital raw extension is mapped correctly`() {
        val expectedMimeType = "expected"
        val node = mock<MegaNode>()
        listOf(
            //Raw
            "3fr", "arw", "cr2",
            "crw", "ciff", "cs1",
            "dcr", "dng", "erf",
            "iiq", "k25", "kdc",
            "mef", "mos", "mrw",
            "nef", "nrw", "orf",
            "pef", "raf", "raw",
            "rw2", "rwl", "sr2",
            "srf", "srw", "x3f",
        ).forEach {
            val expectedExtension = it.uppercase()
            whenever(node.name).thenReturn("withExtension.$expectedExtension")
            assertThat(underTest(node) { expectedMimeType }).isEqualTo(
                RawFileTypeInfo(
                    mimeType = expectedMimeType,
                    extension = expectedExtension
                )
            )
        }
    }

    @Test
    fun `test that a file with text type is mapped correctly`() {
        val expectedExtension = "a text extension"
        val expectedMimeType = "text/any type of text"
        val node = mock<MegaNode> { on { name }.thenReturn("withExtension.$expectedExtension") }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            TextFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension
            )
        )
    }

    @Test
    fun `test all text extensions are mapped correctly`() {
        val expectedMimeType = "expected"
        val node = mock<MegaNode>()
        listOf( //Text
            "txt",
            "ans",
            "ascii",
            "log",
            "wpd",
            "json",
            "md",
            "html",
            "xml",
            "shtml",
            "dhtml",
            "js",
            "css",
            "jar",
            "java",
            "class",
            "php",
            "php3",
            "php4",
            "php5",
            "phtml",
            "inc",
            "asp",
            "pl",
            "cgi",
            "py",
            "sql",
            "accdb",
            "db",
            "dbf",
            "mdb",
            "pdb",
            "c",
            "cpp",
            "h",
            "cs",
            "sh",
            "vb",
            "swift"
        ).forEach {
            val expectedExtension = it
            whenever(node.name).thenReturn("withExtension.$expectedExtension")
            assertThat(underTest(node) { expectedMimeType }).isEqualTo(
                TextFileTypeInfo(
                    mimeType = expectedMimeType,
                    extension = expectedExtension
                )
            )
        }
    }

    @Test
    fun `test that unmapped types returns the unmapped type`() {
        val expectedExtension = "an extension"
        val expectedMimeType = "application/octet-stream"
        val node = mock<MegaNode> { on { name }.thenReturn("withExtension.$expectedExtension") }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            UnMappedFileTypeInfo(
                extension = expectedExtension,
            )
        )
    }

    @Test
    fun `test that other files are mapped as unknown types`() {
        val expectedExtension = "an extension"
        val expectedMimeType = "a mime type"
        val node = mock<MegaNode> { on { name }.thenReturn("withExtension.$expectedExtension") }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            UnknownFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension
            )
        )
    }

    @Test
    fun `test that video files are mapped to the correct type`() {
        val expectedExtension = "mp4"
        val expectedMimeType = "video/mp4"
        val expectedDuration = 120
        val node = mock<MegaNode> {
            on { name }.thenReturn("withExtension.$expectedExtension")
            on { duration }.thenReturn(expectedDuration)
        }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            VideoFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension,
                duration = expectedDuration
            )
        )
    }

    @Test
    fun `test that vob files are mapped to video type`() {
        val expectedExtension = "vob"
        val expectedMimeType = "unknown type"
        val expectedDuration = 120
        val node = mock<MegaNode> {
            on { name }.thenReturn("withExtension.$expectedExtension")
            on { duration }.thenReturn(expectedDuration)
        }

        assertThat(underTest(node) { expectedMimeType }).isEqualTo(
            VideoFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension,
                duration = expectedDuration
            )
        )
    }
}
