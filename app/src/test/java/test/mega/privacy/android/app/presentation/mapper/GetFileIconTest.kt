package test.mega.privacy.android.app.presentation.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.node.model.mapper.getFileIcon
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

class GetFileIconTest {


    @ParameterizedTest(name = "extension {0} should return {1}")
    @MethodSource("provideParameters")
    fun `test that file with extension returns correct icon`(
        extension: String,
        expectedResource: Int,
    ) {
        val fileNode = mock<TypedFileNode> { on { name }.thenReturn("file://example.$extension") }

        assertThat(
            getFileIcon(fileNode = fileNode)
        ).isEqualTo(expectedResource)
    }

    companion object {
        @JvmStatic
        private fun provideParameters(): Stream<Arguments?>? =
            Stream.of(*resources.map { entry ->
                entry.value.map { it to entry.key }
            }.flatten().map {
                Arguments.of(it.first, it.second)
            }.toTypedArray())


        private val resources = mutableMapOf<Int, Array<String>>().also {
            it[R.drawable.ic_3d_thumbnail] = arrayOf("3ds", "3dm", "max", "obj")
            it[R.drawable.ic_aftereffects_thumbnail] =
                arrayOf("aec", "aep", "aepx", "aes", "aet", "aetx")
            it[R.drawable.ic_audio_thumbnail] = arrayOf(
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
            )
            it[R.drawable.ic_cad_thumbnail] = arrayOf("dwg", "dxf")
            it[R.drawable.ic_compressed_thumbnail] = arrayOf(
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
                "7z",
                "bz",
                "bzip2",
                "cab",
                "lha",
                "gzip",
                "ace",
                "arc",
                "pkg"
            )
            it[R.drawable.database_thumbnail] =
                arrayOf("accdb", "db", "dbf", "mdb", "pdb", "sql")
            it[R.drawable.ic_dmg_thumbnail] = arrayOf("dmg")
            it[R.drawable.dreamweaver_thumbnail] = arrayOf("dwt")
            it[R.drawable.ic_excel_thumbnail] =
                arrayOf("xla", "xlam", "xll", "xlm", "xls", "xlsm", "xlsx", "xlt", "xltm", "xltx")
            it[R.drawable.ic_executabe_thumbnail] =
                arrayOf("apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf")
            it[R.drawable.ic_web_lang_thumbnail] = arrayOf("as", "asc", "ascs")
            it[R.drawable.flash_thumbnail] = arrayOf("fla")
            it[R.drawable.ic_font_thumbnail] = arrayOf("fnt", "fon", "otf", "ttf")
            it[R.drawable.gis_thumbnail] = arrayOf("gpx", "kml", "kmz")
            it[R.drawable.html_thumbnail] = arrayOf("dhtml", "htm", "html", "shtml", "xhtml")
            it[R.drawable.ic_illustrator_thumbnail] =
                arrayOf("ai", "aia", "aip", "ait", "art", "irs")
            it[CoreUiR.drawable.ic_image_thumbnail] =
                arrayOf("jpg", "jpeg", "tga", "tif", "tiff", "bmp", "gif", "png")
            it[R.drawable.ic_indesign_thumbnail] = arrayOf("indd")
            it[R.drawable.java_thumbnail] = arrayOf("class", "jar", "java")
            it[R.drawable.midi_thumbnail] = arrayOf("mid", "midi")
            it[R.drawable.ic_pdf_thumbnail] = arrayOf("pdf")
            it[R.drawable.ic_photoshop_thumbnail] = arrayOf("abr", "csh", "psb", "psd")
            it[R.drawable.playlist_thumbnail] = arrayOf("asx", "m3u", "pls")
            it[R.drawable.podcast_thumbnail] = arrayOf("pcast")
            it[R.drawable.ic_powerpoint_thumbnail] = arrayOf(
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
                "pptx"
            )
            it[R.drawable.ic_premiere_thumbnail] = arrayOf("plb", "ppj", "prproj", "prtpset")
            it[R.drawable.ic_raw_thumbnail] = arrayOf(
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
                "srf"
            )
            it[R.drawable.real_audio_thumbnail] = arrayOf("ra", "ram", "rm")
            it[R.drawable.source_thumbnail] =
                arrayOf("c", "cc", "cgi", "cpp", "cxx", "dll", "h", "hpp", "pl", "py", "sh")
            it[R.drawable.ic_spreadsheet_thumbnail] =
                arrayOf("123", "gsheet", "nb", "ots", "sxc", "xlr")
            it[R.drawable.subtitles_thumbnail] = arrayOf("srt")
            it[R.drawable.swf_thumbnail] = arrayOf("swf", "flv")
            it[R.drawable.ic_text_thumbnail] =
                arrayOf("ans", "ascii", "log", "rtf", "txt", "wpd")
            it[R.drawable.ic_torrent_thumbnail] = arrayOf("torrent")
            it[R.drawable.vcard_thumbnail] = arrayOf("vcard", "vcf")
            it[R.drawable.ic_vector_thumbnail] = arrayOf("cdr", "eps", "ps", "svg", "svgz")
            it[R.drawable.ic_video_thumbnail] = arrayOf(
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
                "movie"
            )
            it[R.drawable.video_vob_thumbnail] = arrayOf("vob")
            it[R.drawable.ic_web_data_thumbnail] = arrayOf(
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
                "xml"
            )
            it[R.drawable.ic_word_thumbnail] =
                arrayOf("doc", "docm", "docx", "dot", "dotx", "wps")
            it[R.drawable.ic_pages_thumbnail] = arrayOf("pages")
            it[R.drawable.ic_experiencedesign_thumbnail] = arrayOf("Xd")
            it[R.drawable.ic_keynote_thumbnail] = arrayOf("key")
            it[R.drawable.ic_numbers_thumbnail] = arrayOf("numbers")
            it[R.drawable.ic_openoffice_thumbnail] = arrayOf("odp", "odt", "ods")
            it[R.drawable.ic_sketch_thumbnail] = arrayOf("sketch")
            it[R.drawable.ic_url_thumbnail] = arrayOf("url")
        }
    }
}