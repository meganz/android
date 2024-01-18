package test.mega.privacy.android.app.presentation.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.node.model.mapper.getFileIconOutline
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

class GetFileIconOutlineTest {
    @ParameterizedTest(name = "extension {0} should return {1}")
    @MethodSource("provideParameters")
    fun `test that file with extension returns correct icon`(
        extension: String,
        expectedResource: Int,
    ) {
        val fileNode = mock<TypedFileNode> {
            on { type }.thenReturn(
                UnknownFileTypeInfo(
                    mimeType = "application/$extension",
                    extension = extension
                )
            )
        }

        assertThat(
            getFileIconOutline(fileNode = fileNode)
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
            it[R.drawable.ic_3d_thumbnail_outline] = arrayOf("3ds", "3dm", "max", "obj")
            it[R.drawable.ic_aftereffects_thumbnail_outline] =
                arrayOf("aec", "aep", "aepx", "aes", "aet", "aetx")
            it[R.drawable.ic_audio_thumbnail_outline] = arrayOf(
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
            it[R.drawable.ic_cad_thumbnail_outline] = arrayOf("dwg", "dxf")
            it[R.drawable.ic_compressed_thumbnail_outline] = arrayOf(
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
            it[R.drawable.ic_dmg_thumbnail_outline] = arrayOf("dmg")
            it[R.drawable.ic_excel_thumbnail_outline] =
                arrayOf("xla", "xlam", "xll", "xlm", "xls", "xlsm", "xlsx", "xlt", "xltm", "xltx")
            it[R.drawable.ic_executabe_thumbnail_outline] =
                arrayOf("apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf")
            it[R.drawable.ic_web_lang_thumbnail_outline] = arrayOf("as", "asc", "ascs")
            it[R.drawable.ic_font_thumbnail_outline] = arrayOf("fnt", "fon", "otf", "ttf")
            it[R.drawable.ic_illustrator_thumbnail_outline] =
                arrayOf("ai", "aia", "aip", "ait", "art", "irs")
            it[R.drawable.ic_image_thumbnail_outline] =
                arrayOf("jpg", "jpeg", "tga", "tif", "tiff", "bmp", "gif", "png")
            it[R.drawable.ic_indesign_thumbnail_outline] = arrayOf("indd")
            it[R.drawable.ic_pdf_thumbnail_outline] = arrayOf("pdf")
            it[R.drawable.ic_photoshop_thumbnail_outline] = arrayOf("abr", "csh", "psb", "psd")
            it[R.drawable.ic_powerpoint_thumbnail_outline] = arrayOf(
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
            it[R.drawable.ic_premiere_thumbnail_outline] =
                arrayOf("plb", "ppj", "prproj", "prtpset")
            it[R.drawable.ic_raw_thumbnail_outline] = arrayOf(
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
            it[R.drawable.ic_spreadsheet_thumbnail_outline] =
                arrayOf("123", "gsheet", "nb", "ots", "sxc", "xlr")
            it[R.drawable.ic_text_thumbnail_outline] =
                arrayOf("ans", "ascii", "log", "rtf", "txt", "wpd")
            it[R.drawable.ic_torrent_thumbnail_outline] = arrayOf("torrent")
            it[R.drawable.ic_vector_thumbnail_outline] = arrayOf("cdr", "eps", "ps", "svg", "svgz")
            it[R.drawable.ic_video_thumbnail_outline] = arrayOf(
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
            it[R.drawable.ic_web_data_thumbnail_outline] = arrayOf(
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
            it[R.drawable.ic_word_thumbnail_outline] =
                arrayOf("doc", "docm", "docx", "dot", "dotx", "wps")
            it[R.drawable.ic_pages_thumbnail_outline] = arrayOf("pages")
            it[R.drawable.ic_experiencedesign_thumbnail_outline] = arrayOf("Xd")
            it[R.drawable.ic_keynote_thumbnail_outline] = arrayOf("key")
            it[R.drawable.ic_numbers_thumbnail_outline] = arrayOf("numbers")
            it[R.drawable.ic_openoffice_thumbnail_outline] = arrayOf("odp", "odt", "ods")
            it[R.drawable.ic_url_thumbnail_outline] = arrayOf("url")
        }
    }
}