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
            val iconRes = underTest(typeFileNode)
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
        val iconRes = underTest(typeFileNode)
        Truth.assertThat(iconRes).isEqualTo(R.drawable.ic_generic_list)
    }

    @Test
    fun `test that case random cased extension file type will return default icon`() {
        val typeFileNode = mock<TypedFileNode>()
        val fileTypeInfo = mock<UnknownFileTypeInfo> { fileType ->
            whenever(fileType.extension).thenReturn("JpeG")
        }
        whenever(typeFileNode.type).thenReturn(fileTypeInfo)
        val iconRes = underTest(typeFileNode)
        Truth.assertThat(iconRes).isEqualTo(R.drawable.ic_image_list)
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
                R.drawable.ic_text_list
            } + arrayOf("3ds", "3dm", "max", "obj").associateWith {
                R.drawable.ic_3d_list
            } + arrayOf(
                "aec",
                "aep",
                "aepx",
                "aes",
                "aet",
                "aetx"
            ).associateWith {
                R.drawable.ic_aftereffects_list
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
                "weba"
            ).associateWith {
                R.drawable.ic_audio_list
            } + arrayOf("dwg", "dxf").associateWith {
                R.drawable.ic_cad_list
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
                R.drawable.ic_compressed_list
            } + arrayOf(
                "accdb",
                "db",
                "dbf",
                "mdb",
                "pdb",
                "sql"
            ).associateWith {
                R.drawable.ic_database_list
            } + mutableMapOf(Pair("dmg", R.drawable.ic_dmg_list)) + mutableMapOf(
                Pair(
                    "dwt",
                    R.drawable.ic_dreamweaver_list
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
                R.drawable.ic_excel_list
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
                R.drawable.ic_executable_list
            } + arrayOf("as", "asc", "ascs").associateWith {
                R.drawable.ic_web_lang_list
            } + mutableMapOf(Pair("flg", R.drawable.ic_flash_list)) + arrayOf(
                "fnt",
                "fon",
                "otf",
                "ttf"
            ).associateWith { R.drawable.ic_font_list } + arrayOf(
                "gpx",
                "kml",
                "kmz"
            ).associateWith { R.drawable.ic_gis_list } + arrayOf(
                "dhtml",
                "htm",
                "html",
                "shtml",
                "xhtml"
            ).associateWith { R.drawable.ic_html_list } + arrayOf(
                "ai",
                "aia",
                "aip",
                "ait",
                "art",
                "irs"
            ).associateWith { R.drawable.ic_illustrator_list } + arrayOf(
                "jpg",
                "jpeg",
                "tga",
                "tif",
                "tiff",
                "bmp",
                "gif",
                "png"
            ).associateWith { R.drawable.ic_image_list } + mutableMapOf(
                Pair(
                    "indd",
                    R.drawable.ic_indesign_list
                )
            ) + arrayOf("class", "jar", "java").associateWith {
                R.drawable.ic_java_list
            } + arrayOf(
                "mid",
                "midi"
            ).associateWith { R.drawable.ic_midi_list } + mutableMapOf(
                Pair(
                    "pdf",
                    R.drawable.ic_pdf_list
                )
            ) + arrayOf(
                "abr",
                "csh",
                "psb",
                "psd"
            ).associateWith { R.drawable.ic_photoshop_list } + arrayOf(
                "asx",
                "m3u",
                "pls"
            ).associateWith { R.drawable.ic_playlist_list } +
                    mutableMapOf(Pair("pcast", R.drawable.ic_podcast_list)) + arrayOf(
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
            ).associateWith { R.drawable.ic_powerpoint_list } + arrayOf(
                "plb",
                "ppj",
                "prproj",
                "prtpset"
            ).associateWith { R.drawable.ic_premiere_list } + arrayOf(
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
            ).associateWith { R.drawable.ic_raw_list } + arrayOf(
                "ra",
                "ram",
                "rm"
            ).associateWith { R.drawable.ic_real_audio_list } + arrayOf(
                "c",
                "cc",
                "cgi",
                "cpp",
                "cxx",
                "dll",
                "h",
                "hpp",
                "pl",
                "py",
                "sh",
                "cs",
                "swift"
            ).associateWith { R.drawable.ic_source_list } + arrayOf(
                "123",
                "gsheet",
                "nb",
                "ods",
                "ots",
                "sxc",
                "xlr"
            ).associateWith { R.drawable.ic_spreadsheet_list } + mutableMapOf(
                Pair(
                    "srt",
                    R.drawable.ic_subtitles_list
                )
            ) + arrayOf(
                "swf",
                "flv"
            ).associateWith { R.drawable.ic_swf_list } + mutableMapOf(
                Pair(
                    "torrent",
                    R.drawable.ic_torrent_list
                )
            ) + arrayOf("vcard", "vcf").associateWith { R.drawable.ic_vcard_list } + arrayOf(
                "cdr",
                "eps",
                "ps",
                "svg",
                "svgz"
            ).associateWith { R.drawable.ic_vector_list } + arrayOf(
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
            ).associateWith { R.drawable.ic_video_list } + mutableMapOf(
                Pair(
                    "vob",
                    R.drawable.ic_video_vob_list
                )
            ) + arrayOf(
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
            ).associateWith { R.drawable.ic_web_data_list } + arrayOf(
                "doc",
                "docm",
                "docx",
                "dot",
                "dotx",
                "wps"
            ).associateWith { R.drawable.ic_word_list } + mutableMapOf(
                Pair(
                    "pages",
                    R.drawable.ic_pages_list
                )
            ) + mutableMapOf(
                Pair(
                    "Xd",
                    R.drawable.ic_experiencedesign_list
                )
            ) + mutableMapOf(Pair("key", R.drawable.ic_keynote_list)) + mutableMapOf(
                Pair(
                    "numbers",
                    R.drawable.ic_numbers_list
                )
            ) + mutableMapOf(Pair("sketch", R.drawable.ic_sketch_list)) + mutableMapOf(
                Pair(
                    "url",
                    R.drawable.ic_url_list
                )
            ) + arrayOf("odp", "odt", "ods").associateWith { R.drawable.ic_openoffice_list }
    }
}