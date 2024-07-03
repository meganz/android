package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileTypeInfoMapperTest @Inject constructor() {
    private lateinit var underTest: FileTypeInfoMapper
    private val mimeTypeMapper = mock<MimeTypeMapper>()

    @BeforeAll
    fun setUp() {
        underTest = FileTypeInfoMapper(mimeTypeMapper)
    }

    @Test
    fun `test that a node with no extension returns UnMappedFileTypeInfo`() {
        val expectedMimeType = "a mime type"
        val name = "NoExtension."
        whenever(mimeTypeMapper(anyOrNull())).thenReturn(expectedMimeType)

        assertThat(underTest(name, 0)).isEqualTo(UnMappedFileTypeInfo(""))
    }

    @Test
    fun `test that extension is mapped if present`() {
        val expectedMimeType = "type"
        val expectedExtension = "txt"
        val name = "withExtension.$expectedExtension"
        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)

        assertThat(underTest(name, 0).extension).isEqualTo(expectedExtension)
    }

    @Test
    fun `test that a pdf is mapped correctly`() {
        val expectedMimeType = "application/pdf"
        val expectedExtension = "pdf"
        val name = "withExtension.pdf"
        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, 0)).isEqualTo(PdfFileTypeInfo)
    }

    @Test
    fun `test that a zip file is mapped correctly`() {
        val expectedExtension = "zip"
        val expectedMimeType = "application/zip"
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, 0)).isEqualTo(
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
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, 0)).isEqualTo(
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
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, 0)).isEqualTo(
            UrlFileTypeInfo
        )
    }

    @Test
    fun `test that an image file is mapped correctly`() {
        val expectedExtension = "jpg"
        val expectedMimeType = "image/jpeg"
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, 0)).isEqualTo(
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
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, expectedDuration)).isEqualTo(
            AudioFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension,
                duration = expectedDuration.seconds
            )
        )
    }

    @Test
    fun `test that a file with opus extension is mapped correctly`() {
        val expectedExtension = "opus"
        val expectedMimeType = "opus type"
        val expectedDuration = 120
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, expectedDuration)).isEqualTo(
            AudioFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension,
                duration = expectedDuration.seconds,
            )
        )
    }

    @Test
    fun `test that a file with weba extension is mapped correctly`() {
        val expectedExtension = "weba"
        val expectedMimeType = "weba type"
        val expectedDuration = 120
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, expectedDuration)).isEqualTo(
            AudioFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension,
                duration = expectedDuration.seconds,
            )
        )
    }

    @Test
    fun `test that a file with gif extension is mapped correctly`() {
        val expectedExtension = "gif"
        val expectedMimeType = "image/gif"
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, 0)).isEqualTo(
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
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, 0)).isEqualTo(
            GifFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension
            )
        )
    }

    @Test
    fun `test that a file with raw extension is mapped correctly`() {
        val expectedMimeType = "expected"
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
            val name = "withExtension.$expectedExtension"

            whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
            assertThat(underTest(name, 0)).isEqualTo(
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
            val name = "withExtension.$expectedExtension"

            whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
            assertThat(underTest(name, 0)).isEqualTo(
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
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, 0)).isEqualTo(
            TextFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension
            )
        )
    }

    @Test
    fun `test all text extensions are mapped correctly`() {
        val expectedMimeType = "expected"
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
            val name = "withExtension.$expectedExtension"

            whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
            assertThat(underTest(name, 0)).isEqualTo(
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
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, 0)).isEqualTo(
            UnMappedFileTypeInfo(
                extension = expectedExtension,
            )
        )
    }

    @Test
    fun `test that other files are mapped as unknown types`() {
        val expectedExtension = "an extension"
        val expectedMimeType = "a mime type"
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, 0)).isEqualTo(
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
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, expectedDuration)).isEqualTo(
            VideoFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension,
                duration = expectedDuration.seconds
            )
        )
    }

    @Test
    fun `test that vob files are mapped to video type`() {
        val expectedExtension = "vob"
        val expectedMimeType = "unknown type"
        val expectedDuration = 120
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, expectedDuration)).isEqualTo(
            VideoFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension,
                duration = expectedDuration.seconds
            )
        )
    }

    @Test
    fun `test that a file with svg extension is mapped correctly`() {
        val expectedMimeType = "expected"
        val expectedExtension = "svg"
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, 0)).isEqualTo(
            SvgFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension
            )
        )

    }

    @Test
    fun `test that a file with capital svg extension is mapped correctly`() {
        val expectedMimeType = "expected"
        val expectedExtension = "SVG"
        val name = "withExtension.$expectedExtension"

        whenever(mimeTypeMapper(expectedExtension)).thenReturn(expectedMimeType)
        assertThat(underTest(name, 0)).isEqualTo(
            SvgFileTypeInfo(
                mimeType = expectedMimeType,
                extension = expectedExtension
            )
        )
    }
}
