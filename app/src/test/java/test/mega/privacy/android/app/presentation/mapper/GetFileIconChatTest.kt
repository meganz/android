package test.mega.privacy.android.app.presentation.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.node.model.mapper.getFileIconChat
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.icon.pack.R
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class GetFileIconChatTest {
    @ParameterizedTest(name = "extension {0} should return {1}")
    @MethodSource("provideParameters")
    fun `test that file with extension returns correct icon`(
        extension: String,
        expectedResource: Int,
    ) {
        val type = UnknownFileTypeInfo(
            mimeType = "application/$extension",
            extension = extension
        )

        assertThat(
            getFileIconChat(type)
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
            it[R.drawable.ic_3d_medium_solid] = arrayOf("3ds", "3dm", "max", "obj")
            it[R.drawable.ic_aftereffects_medium_solid] =
                arrayOf("aec", "aep", "aepx", "aes", "aet", "aetx")
            it[R.drawable.ic_audio_medium_solid] = arrayOf(
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
            it[R.drawable.ic_cad_medium_solid] = arrayOf("dwg", "dxf")
            it[R.drawable.ic_compressed_medium_solid] = arrayOf(
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
            it[R.drawable.ic_dmg_medium_solid] = arrayOf("dmg")
            it[R.drawable.ic_excel_medium_solid] =
                arrayOf("xla", "xlam", "xll", "xlm", "xls", "xlsm", "xlsx", "xlt", "xltm", "xltx")
            it[R.drawable.ic_executable_medium_solid] =
                arrayOf("apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf")
            it[R.drawable.ic_web_lang_medium_solid] = arrayOf("as", "asc", "ascs")
            it[R.drawable.ic_font_medium_solid] = arrayOf("fnt", "fon", "otf", "ttf")
            it[R.drawable.ic_illustrator_medium_solid] =
                arrayOf("ai", "aia", "aip", "ait", "art", "irs")
            it[R.drawable.ic_image_medium_solid] =
                arrayOf("jpg", "jpeg", "tga", "tif", "tiff", "bmp", "gif", "png")
            it[R.drawable.ic_indesign_medium_solid] = arrayOf("indd")
            it[R.drawable.ic_pdf_medium_solid] = arrayOf("pdf")
            it[R.drawable.ic_photoshop_medium_solid] = arrayOf("abr", "csh", "psb", "psd")
            it[R.drawable.ic_powerpoint_medium_solid] = arrayOf(
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
            it[R.drawable.ic_premiere_medium_solid] =
                arrayOf("plb", "ppj", "prproj", "prtpset")
            it[R.drawable.ic_raw_medium_solid] = arrayOf(
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
            it[R.drawable.ic_spreadsheet_medium_solid] =
                arrayOf("123", "gsheet", "nb", "ots", "sxc", "xlr")
            it[R.drawable.ic_text_medium_solid] =
                arrayOf("ans", "ascii", "log", "rtf", "txt", "wpd")
            it[R.drawable.ic_torrent_medium_solid] = arrayOf("torrent")
            it[R.drawable.ic_vector_medium_solid] = arrayOf("cdr", "eps", "ps", "svg", "svgz")
            it[R.drawable.ic_video_medium_solid] = arrayOf(
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
            it[R.drawable.ic_web_data_medium_solid] = arrayOf(
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
            it[R.drawable.ic_word_medium_solid] =
                arrayOf("doc", "docm", "docx", "dot", "dotx", "wps")
            it[R.drawable.ic_pages_medium_solid] = arrayOf("pages")
            it[R.drawable.ic_experiencedesign_medium_solid] = arrayOf("Xd")
            it[R.drawable.ic_keynote_medium_solid] = arrayOf("key")
            it[R.drawable.ic_numbers_medium_solid] = arrayOf("numbers")
            it[R.drawable.ic_openoffice_medium_solid] = arrayOf("odp", "odt", "ods")
            it[R.drawable.ic_url_medium_solid] = arrayOf("url")
        }
    }
}