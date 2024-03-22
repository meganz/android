package mega.privacy.android.feature.sync.ui.mapper

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.icon.pack.R
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FileTypeIconMapperTest {
    private val underTest = FileTypeIconMapper()

    @Test
    fun `test that proper icon Resource is returned when File type is provided`() {
        extensionMap.forEach {
            val typeFileNode = mock<TypedFileNode>()
            val fileTypeInfo = mock<UnknownFileTypeInfo> { fileType ->
                whenever(fileType.extension).thenReturn(it.key)
            }
            whenever(typeFileNode.type).thenReturn(fileTypeInfo)
            val iconRes = underTest(typeFileNode.type.extension)
            Truth.assertThat(iconRes).isEqualTo(it.value)
        }
    }

    @Test
    fun `test that unknown file type will return default icon`() {
        val typeFileNode = mock<TypedFileNode>()
        val fileTypeInfo = mock<UnknownFileTypeInfo> { fileType ->
            whenever(fileType.extension).thenReturn("unknownExtension")
        }
        whenever(typeFileNode.type).thenReturn(fileTypeInfo)
        val iconRes = underTest(typeFileNode.type.extension)
        Truth.assertThat(iconRes).isEqualTo(R.drawable.ic_generic_medium_solid)
    }

    @Test
    fun `test that case random cased extension file type will return default icon`() {
        val typeFileNode = mock<TypedFileNode>()
        val fileTypeInfo = mock<UnknownFileTypeInfo> { fileType ->
            whenever(fileType.extension).thenReturn("JpeG")
        }
        whenever(typeFileNode.type).thenReturn(fileTypeInfo)
        val iconRes = underTest(typeFileNode.type.extension)
        Truth.assertThat(iconRes).isEqualTo(R.drawable.ic_image_medium_solid)
    }

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
                R.drawable.ic_text_medium_solid
            } + arrayOf("3ds", "3dm", "max", "obj").associateWith {
                R.drawable.ic_3d_medium_solid
            } + arrayOf(
                "aec",
                "aep",
                "aepx",
                "aes",
                "aet",
                "aetx"
            ).associateWith {
                R.drawable.ic_aftereffects_medium_solid
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
                R.drawable.ic_audio_medium_solid
            } + arrayOf("dwg", "dxf").associateWith {
                R.drawable.ic_cad_medium_solid
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
                R.drawable.ic_compressed_medium_solid
            } + mutableMapOf(Pair("dmg", R.drawable.ic_dmg_medium_solid)) + arrayOf(
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
                R.drawable.ic_excel_medium_solid
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
                R.drawable.ic_executable_medium_solid
            } + arrayOf("as", "asc", "ascs").associateWith {
                R.drawable.ic_web_lang_medium_solid
            } + arrayOf(
                "fnt",
                "fon",
                "otf",
                "ttf"
            ).associateWith { R.drawable.ic_font_medium_solid } + arrayOf(
                "ai",
                "aia",
                "aip",
                "ait",
                "art",
                "irs"
            ).associateWith { R.drawable.ic_illustrator_medium_solid } + arrayOf(
                "jpg",
                "jpeg",
                "tga",
                "tif",
                "tiff",
                "bmp",
                "gif",
                "png"
            ).associateWith { R.drawable.ic_image_medium_solid } + mutableMapOf(
                Pair(
                    "indd",
                    R.drawable.ic_indesign_medium_solid
                )
            ) + mutableMapOf(
                Pair(
                    "pdf",
                    R.drawable.ic_pdf_medium_solid
                )
            ) + arrayOf(
                "abr",
                "csh",
                "psb",
                "psd"
            ).associateWith { R.drawable.ic_photoshop_medium_solid } +
                    arrayOf(
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
                    ).associateWith { R.drawable.ic_powerpoint_medium_solid } + arrayOf(
                "plb",
                "ppj",
                "prproj",
                "prtpset"
            ).associateWith { R.drawable.ic_premiere_medium_solid } + arrayOf(
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
            ).associateWith { R.drawable.ic_raw_medium_solid } + arrayOf(
                "123",
                "gsheet",
                "nb",
                "ods",
                "ots",
                "sxc",
                "xlr"
            ).associateWith { R.drawable.ic_spreadsheet_medium_solid } + mutableMapOf(
                Pair(
                    "torrent",
                    R.drawable.ic_torrent_medium_solid
                )
            ) + arrayOf(
                "cdr",
                "eps",
                "ps",
                "svg",
                "svgz"
            ).associateWith { R.drawable.ic_vector_medium_solid } + arrayOf(
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
            ).associateWith { R.drawable.ic_video_medium_solid } + arrayOf(
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
            ).associateWith { R.drawable.ic_web_data_medium_solid } + arrayOf(
                "doc",
                "docm",
                "docx",
                "dot",
                "dotx",
                "wps"
            ).associateWith { R.drawable.ic_word_medium_solid } + mutableMapOf(
                Pair(
                    "pages",
                    R.drawable.ic_pages_medium_solid
                )
            ) + mutableMapOf(
                Pair(
                    "Xd",
                    R.drawable.ic_experiencedesign_medium_solid
                )
            ) + mutableMapOf(Pair("key", R.drawable.ic_keynote_medium_solid)) + mutableMapOf(
                Pair(
                    "numbers",
                    R.drawable.ic_numbers_medium_solid
                )
            ) + mutableMapOf(
                Pair(
                    "url",
                    R.drawable.ic_url_medium_solid
                )
            ) + arrayOf("odp", "odt", "ods").associateWith { R.drawable.ic_openoffice_medium_solid }
    }
}