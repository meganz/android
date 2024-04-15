package mega.privacy.android.core.ui.mapper

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.icon.pack.R
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileTypeIconMapperTest {
    private val underTest = FileTypeIconMapper()

    @ParameterizedTest(name = "Icon type is {0} ")
    @MethodSource("provideParams")
    fun `test that proper icon Resource is returned when File type is provided`(
        iconType: IconType,
    ) {
        extensionMap.forEach {
            val typeFileNode = mock<TypedFileNode>()
            val fileTypeInfo = mock<UnknownFileTypeInfo> { fileType ->
                whenever(fileType.extension).thenReturn(it.key)
            }
            whenever(typeFileNode.type).thenReturn(fileTypeInfo)
            val iconRes = underTest(typeFileNode.type.extension, iconType)
            Truth.assertThat(iconRes)
                .isEqualTo(if (iconType == IconType.Solid) it.value[0] else it.value[1])
        }
    }

    @ParameterizedTest(name = "Icon type is {0} ")
    @MethodSource("provideParams")
    fun `test that unknown file type will return default icon`(iconType: IconType) {
        val typeFileNode = mock<TypedFileNode>()
        val fileTypeInfo = mock<UnknownFileTypeInfo> { fileType ->
            whenever(fileType.extension).thenReturn("unknownExtension")
        }
        whenever(typeFileNode.type).thenReturn(fileTypeInfo)
        val iconRes = underTest(typeFileNode.type.extension, iconType)
        Truth.assertThat(iconRes)
            .isEqualTo(R.drawable.ic_generic_medium_solid)
    }

    @ParameterizedTest(name = "Icon type is {0} ")
    @MethodSource("provideParams")
    fun `test that case random cased extension file type will return default icon`(
        iconType: IconType,
    ) {
        val typeFileNode = mock<TypedFileNode>()
        val fileTypeInfo = mock<UnknownFileTypeInfo> { fileType ->
            whenever(fileType.extension).thenReturn("JpeG")
        }
        whenever(typeFileNode.type).thenReturn(fileTypeInfo)
        val iconRes = underTest(typeFileNode.type.extension, iconType)
        if (iconType == IconType.Solid) {
            Truth.assertThat(iconRes).isEqualTo(
                R.drawable.ic_image_medium_solid
            )
        } else {
            Truth.assertThat(iconRes).isEqualTo(
                R.drawable.ic_image_thumbnail_outline
            )
        }
    }

    private fun provideParams(): Stream<Arguments> = Stream.of(
        Arguments.of(IconType.Solid),
        Arguments.of(IconType.Outlined)
    )

    companion object {
        private val extensionMap =
            arrayOf(
                "txt",
                "ans",
                "ascii",
                "log",
                "wpd",
                "json",
                "rtf"
            ).associateWith {
                listOf(R.drawable.ic_text_medium_solid, R.drawable.ic_text_thumbnail_outline)
            } + arrayOf("3ds", "3dm", "max", "obj").associateWith {
                listOf(R.drawable.ic_3d_medium_solid, R.drawable.ic_3d_thumbnail_outline)
            } + arrayOf(
                "aec",
                "aep",
                "aepx",
                "aes",
                "aet",
                "aetx"
            ).associateWith {
                listOf(
                    R.drawable.ic_aftereffects_medium_solid,
                    R.drawable.ic_aftereffects_thumbnail_outline
                )
            } + arrayOf(
                "aif",
                "aiff",
                "wav",
                "flac",
                "iff",
                "m4a",
                "wma",
                "oga",
                "ogg",
                "mp3",
                "3ga",
                "opus",
                "weba",
                "ra",
                "ram",
                "rm"
            ).associateWith {
                listOf(R.drawable.ic_audio_medium_solid, R.drawable.ic_audio_thumbnail_outline)
            } + arrayOf("dwg", "dxf").associateWith {
                listOf(R.drawable.ic_cad_medium_solid, R.drawable.ic_cad_thumbnail_outline)
            } + arrayOf(
                "bz2",
                "gz",
                "rar",
                "tar",
                "tbz",
                "tgz",
                "zip",
                "deb",
                "udeb",
                "rpm",
                "air",
                "apk",
                "dmg",
                "7z",
                "bz",
                "bzip2",
                "cab",
                "lha",
                "gzip",
                "ace",
                "arc",
                "pkg"
            ).associateWith {
                listOf(
                    R.drawable.ic_compressed_medium_solid,
                    R.drawable.ic_compressed_thumbnail_outline
                )
            } + mutableMapOf(
                Pair(
                    "dmg",
                    listOf(R.drawable.ic_dmg_medium_solid, R.drawable.ic_dmg_thumbnail_outline)
                )
            ) + arrayOf(
                "xla",
                "xlam",
                "xll",
                "xlm",
                "xls",
                "xlsm",
                "xlsx",
                "xlt",
                "xltm",
                "xltx"
            ).associateWith {
                listOf(R.drawable.ic_excel_medium_solid, R.drawable.ic_excel_thumbnail_outline)
            } + arrayOf(
                "apk",
                "app",
                "bat",
                "com",
                "exe",
                "gadget",
                "msi",
                "pif",
                "vb",
                "wsf"
            ).associateWith {
                listOf(
                    R.drawable.ic_executable_medium_solid,
                    R.drawable.ic_executable_thumbnail_outline
                )
            } + arrayOf("as", "asc", "ascs").associateWith {
                listOf(
                    R.drawable.ic_web_lang_medium_solid,
                    R.drawable.ic_web_lang_thumbnail_outline
                )
            } + arrayOf(
                "fnt",
                "fon",
                "otf",
                "ttf"
            ).associateWith {
                listOf(
                    R.drawable.ic_font_medium_solid, R.drawable.ic_font_thumbnail_outline
                )
            } + arrayOf(
                "ai",
                "aia",
                "aip",
                "ait",
                "art",
                "irs"
            ).associateWith {
                listOf(
                    R.drawable.ic_illustrator_medium_solid,
                    R.drawable.ic_illustrator_thumbnail_outline
                )
            } + arrayOf(
                "jpg",
                "jpeg",
                "tga",
                "tif",
                "tiff",
                "bmp",
                "gif",
                "png"
            ).associateWith {
                listOf(
                    R.drawable.ic_image_medium_solid,
                    R.drawable.ic_image_thumbnail_outline
                )
            } + mutableMapOf(
                Pair(
                    "indd",
                    listOf(
                        R.drawable.ic_indesign_medium_solid,
                        R.drawable.ic_indesign_thumbnail_outline
                    )
                )
            ) + mutableMapOf(
                Pair(
                    "pdf",
                    listOf(
                        R.drawable.ic_pdf_medium_solid,
                        R.drawable.ic_pdf_thumbnail_outline
                    )
                )
            ) + arrayOf(
                "abr",
                "csh",
                "psb",
                "psd"
            ).associateWith {
                listOf(
                    R.drawable.ic_photoshop_medium_solid,
                    R.drawable.ic_photoshop_thumbnail_outline
                )
            } + arrayOf(
                "pot",
                "potm",
                "potx",
                "ppam",
                "ppc",
                "pps",
                "ppsm",
                "ppsx",
                "ppt",
                "pptm",
                "pptx",
            ).associateWith {
                listOf(
                    R.drawable.ic_powerpoint_medium_solid,
                    R.drawable.ic_powerpoint_thumbnail_outline
                )
            } + arrayOf(
                "plb",
                "ppj",
                "prproj",
                "prtpset"
            ).associateWith {
                listOf(
                    R.drawable.ic_premiere_medium_solid,
                    R.drawable.ic_premiere_thumbnail_outline
                )
            } + arrayOf(
                "3fr",
                "mef",
                "arw",
                "bay",
                "cr2",
                "dcr",
                "dng",
                "erf",
                "fff",
                "mrw",
                "nef",
                "orf",
                "pef",
                "rw2",
                "rwl",
                "srf",
                "iiq",
                "k25",
                "kdc",
                "mos",
                "raw",
                "sr2",
                "x3f",
                "cr3",
                "ciff",
            ).associateWith {
                listOf(
                    R.drawable.ic_raw_medium_solid,
                    R.drawable.ic_raw_thumbnail_outline
                )
            } + arrayOf(
                "123",
                "gsheet",
                "nb",
                "ods",
                "ots",
                "sxc",
                "xlr"
            ).associateWith {
                listOf(
                    R.drawable.ic_spreadsheet_medium_solid,
                    R.drawable.ic_spreadsheet_thumbnail_outline
                )
            } + mutableMapOf(
                Pair(
                    "torrent",
                    listOf(
                        R.drawable.ic_torrent_medium_solid,
                        R.drawable.ic_torrent_thumbnail_outline
                    )
                )
            ) + arrayOf(
                "cdr",
                "eps",
                "ps",
                "svg",
                "svgz"
            ).associateWith {
                listOf(
                    R.drawable.ic_vector_medium_solid,
                    R.drawable.ic_vector_thumbnail_outline
                )
            } + arrayOf(
                "3g2",
                "3gp",
                "asf",
                "avi",
                "mkv",
                "mov",
                "mpeg",
                "mpg",
                "wmv",
                "3gpp",
                "h261",
                "h263",
                "h264",
                "jpgv",
                "jpm",
                "jpgm",
                "mp4",
                "mp4v",
                "mpg4",
                "mpe",
                "m1v",
                "m2v",
                "ogv",
                "qt",
                "m4u",
                "webm",
                "f4v",
                "fli",
                "m4v",
                "mkv",
                "mk3d",
                "movie",
                "vob"
            ).associateWith {
                listOf(
                    R.drawable.ic_video_medium_solid,
                    R.drawable.ic_video_thumbnail_outline
                )
            } + arrayOf(
                "asp",
                "aspx",
                "php",
                "php3",
                "php4",
                "php5",
                "phtml",
                "css",
                "inc",
                "js",
                "xml",
            ).associateWith {
                listOf(
                    R.drawable.ic_web_data_medium_solid,
                    R.drawable.ic_web_data_thumbnail_outline
                )
            } + arrayOf(
                "doc",
                "docm",
                "docx",
                "dot",
                "dotx",
                "wps"
            ).associateWith {
                listOf(
                    R.drawable.ic_word_medium_solid,
                    R.drawable.ic_word_thumbnail_outline
                )
            } + mutableMapOf(
                Pair(
                    "pages",
                    listOf(
                        R.drawable.ic_pages_medium_solid,
                        R.drawable.ic_pages_thumbnail_outline
                    )
                )
            ) + mutableMapOf(
                Pair(
                    "Xd",
                    listOf(
                        R.drawable.ic_experiencedesign_medium_solid,
                        R.drawable.ic_experiencedesign_thumbnail_outline
                    )
                )
            ) + mutableMapOf(
                Pair(
                    "key",
                    listOf(
                        R.drawable.ic_keynote_medium_solid,
                        R.drawable.ic_keynote_thumbnail_outline
                    )
                )
            ) + mutableMapOf(
                Pair(
                    "numbers",
                    listOf(
                        R.drawable.ic_numbers_medium_solid,
                        R.drawable.ic_numbers_thumbnail_outline
                    )
                )
            ) + mutableMapOf(
                Pair(
                    "url",
                    listOf(
                        R.drawable.ic_url_medium_solid,
                        R.drawable.ic_url_thumbnail_outline
                    )
                )
            ) + arrayOf(
                "odp",
                "odt",
                "ods"
            ).associateWith {
                listOf(
                    R.drawable.ic_openoffice_medium_solid,
                    R.drawable.ic_openoffice_thumbnail_outline
                )
            }
    }
}