package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
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
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Locale
import kotlin.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSyncMediaTypeUseCaseTest {

    private lateinit var underTest: GetSyncMediaTypeUseCase

    private val getFileTypeInfoByNameUseCase = mock<GetFileTypeInfoByNameUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetSyncMediaTypeUseCase(
            getFileTypeInfoByNameUseCase = getFileTypeInfoByNameUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getFileTypeInfoByNameUseCase)
    }

    private fun stubFileTypeInfo(info: FileTypeInfo) {
        whenever(getFileTypeInfoByNameUseCase(any(), any())).thenReturn(info)
    }

    private fun imageTypes() = listOf(
        StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg"),
        GifFileTypeInfo(mimeType = "image/gif", extension = "gif"),
        RawFileTypeInfo(mimeType = "image/x-canon-cr2", extension = "cr2"),
        SvgFileTypeInfo(mimeType = "image/svg+xml", extension = "svg"),
    )

    private fun documentTypes() = listOf(
        PdfFileTypeInfo,
        TextFileTypeInfo(mimeType = "text/plain", extension = "txt"),
    )

    private fun otherTypes() = listOf(
        UrlFileTypeInfo,
        UnMappedFileTypeInfo(extension = "bin"),
        UnknownFileTypeInfo(mimeType = "application/json", extension = "json"),
        UnknownFileTypeInfo(mimeType = "application/octet-stream", extension = "exe"),
        UnknownFileTypeInfo(mimeType = "application/vnd.ms-fontobject", extension = "eot"),
        UnknownFileTypeInfo(mimeType = "application/vnd.ms-pki.stl", extension = "stl"),
        UnknownFileTypeInfo(mimeType = "", extension = ""),
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("imageTypes")
    fun `test that invoke returns Image when the file type is an image`(
        info: FileTypeInfo,
    ) = runTest {
        stubFileTypeInfo(info)

        assertThat(underTest("file")).isEqualTo(SyncMediaType.Image)
    }

    @Test
    fun `test that invoke returns Video when the file type is a video`() = runTest {
        stubFileTypeInfo(
            VideoFileTypeInfo(mimeType = "video/mp4", extension = "mp4", duration = Duration.ZERO)
        )

        assertThat(underTest("file.mp4")).isEqualTo(SyncMediaType.Video)
    }

    @Test
    fun `test that invoke returns Audio when the file type is an audio`() = runTest {
        stubFileTypeInfo(
            AudioFileTypeInfo(mimeType = "audio/mpeg", extension = "mp3", duration = Duration.ZERO)
        )

        assertThat(underTest("file.mp3")).isEqualTo(SyncMediaType.Audio)
    }

    @Test
    fun `test that invoke returns Archive when the file type is a zip`() = runTest {
        stubFileTypeInfo(ZipFileTypeInfo(mimeType = "application/zip", extension = "zip"))

        assertThat(underTest("file.zip")).isEqualTo(SyncMediaType.Archive)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("documentTypes")
    fun `test that invoke returns Document when the file type is a pdf or text`(
        info: FileTypeInfo,
    ) = runTest {
        stubFileTypeInfo(info)

        assertThat(underTest("file")).isEqualTo(SyncMediaType.Document)
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = [
            "application/x-rar-compressed",
            "application/rar",
            "application/vnd.rar",
            "application/x-7z-compressed",
            "application/x-tar",
            "application/gzip",
            "application/x-gzip",
            "application/x-bzip2",
            "application/x-gtar",
            "application/x-gtar-compressed",
            "application/x-xz",
            "application/zstd",
            "application/x-iso9660-image",
            "application/x-apple-diskimage",
            "application/vnd.ms-cab-compressed",
            "application/x-bittorrent",
        ]
    )
    fun `test that invoke returns Archive when an unknown type has an archive mime type`(
        mimeType: String,
    ) = runTest {
        stubFileTypeInfo(UnknownFileTypeInfo(mimeType = mimeType, extension = "ext"))

        assertThat(underTest("file.ext")).isEqualTo(SyncMediaType.Archive)
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = [
            "application/msword",
            "application/rtf",
            "application/vnd.ms-excel",
            "application/vnd.ms-excel.sheet.macroenabled.12",
            "application/vnd.ms-powerpoint",
            "application/vnd.ms-word.document.macroenabled.12",
            "application/vnd.ms-works",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.apple.pages",
            "application/vnd.apple.numbers",
            "application/vnd.apple.keynote",
        ]
    )
    fun `test that invoke returns Document when an unknown type has a document mime type`(
        mimeType: String,
    ) = runTest {
        stubFileTypeInfo(UnknownFileTypeInfo(mimeType = mimeType, extension = "ext"))

        assertThat(underTest("file.ext")).isEqualTo(SyncMediaType.Document)
    }

    @Test
    fun `test that invoke returns Archive when the archive mime type is upper case`() = runTest {
        stubFileTypeInfo(
            UnknownFileTypeInfo(mimeType = "APPLICATION/X-RAR-COMPRESSED", extension = "RAR")
        )

        assertThat(underTest("FILE.RAR")).isEqualTo(SyncMediaType.Archive)
    }

    @Test
    fun `test that invoke returns Document when the document mime prefix is mixed case`() =
        runTest {
            stubFileTypeInfo(
                UnknownFileTypeInfo(
                    mimeType = "Application/VND.OpenXMLFormats-OfficeDocument.WordProcessingML.Document",
                    extension = "DOCX",
                )
            )

            assertThat(underTest("FILE.DOCX")).isEqualTo(SyncMediaType.Document)
        }

    @Test
    fun `test that invoke returns Archive when the mime type is upper case under a Turkish locale`() =
        runTest {
            val defaultLocale = Locale.getDefault()
            Locale.setDefault(Locale.forLanguageTag("tr"))
            try {
                stubFileTypeInfo(
                    UnknownFileTypeInfo(mimeType = "APPLICATION/X-BITTORRENT", extension = "TORRENT")
                )

                assertThat(underTest("FILE.TORRENT")).isEqualTo(SyncMediaType.Archive)
            } finally {
                Locale.setDefault(defaultLocale)
            }
        }

    @Test
    fun `test that invoke returns Archive when the archive mime type has parameters`() = runTest {
        stubFileTypeInfo(
            UnknownFileTypeInfo(mimeType = "application/x-tar; charset=binary", extension = "tar")
        )

        assertThat(underTest("file.tar")).isEqualTo(SyncMediaType.Archive)
    }

    @Test
    fun `test that invoke returns Document when the document mime type has parameters`() =
        runTest {
            stubFileTypeInfo(
                UnknownFileTypeInfo(mimeType = "application/msword;charset=binary", extension = "doc")
            )

            assertThat(underTest("file.doc")).isEqualTo(SyncMediaType.Document)
        }

    @ParameterizedTest(name = "{0}")
    @MethodSource("otherTypes")
    fun `test that invoke returns Other when the file type is neither media nor archive nor document`(
        info: FileTypeInfo,
    ) = runTest {
        stubFileTypeInfo(info)

        assertThat(underTest("file")).isEqualTo(SyncMediaType.Other)
    }

    @Test
    fun `test that invoke passes the file name unchanged to the file type resolution`() =
        runTest {
            stubFileTypeInfo(PdfFileTypeInfo)

            underTest("Report.PDF")

            verify(getFileTypeInfoByNameUseCase).invoke("Report.PDF", 0)
        }
}
