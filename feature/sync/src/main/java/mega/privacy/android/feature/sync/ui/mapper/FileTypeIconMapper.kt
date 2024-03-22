package mega.privacy.android.feature.sync.ui.mapper

import mega.privacy.android.icon.pack.R as iconPackR
import java.util.TreeMap
import javax.inject.Inject

/**
 * Mapper which will return icon for FileTypeInfo
 */
class FileTypeIconMapper @Inject constructor() {
    /**
     * invoke
     * @param fileExtension [String]
     */
    operator fun invoke(fileExtension: String): Int =
        treeMap[fileExtension] ?: iconPackR.drawable.ic_generic_medium_solid
}

private val textExtensionMap =
    arrayOf(
        "txt",
        "ans",
        "ascii",
        "log",
        "wpd",
        "json",
        "rtf"
    ).associateWith { iconPackR.drawable.ic_text_medium_solid }


private val threeDMap =
    arrayOf("3ds", "3dm", "max", "obj").associateWith { iconPackR.drawable.ic_3d_medium_solid }

private val afterEffectsMap =
    arrayOf(
        "aec",
        "aep",
        "aepx",
        "aes",
        "aet",
        "aetx"
    ).associateWith { iconPackR.drawable.ic_aftereffects_medium_solid }


private val audioMap =
    arrayOf(
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
    ).associateWith { iconPackR.drawable.ic_audio_medium_solid }

private val cadMap = arrayOf("dwg", "dxf").associateWith { iconPackR.drawable.ic_cad_medium_solid }

private val compressedMap = arrayOf(
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
).associateWith { iconPackR.drawable.ic_compressed_medium_solid }


private val dmgMap = mutableMapOf(Pair("dmg", iconPackR.drawable.ic_dmg_medium_solid))


private val excelMap = arrayOf(
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
).associateWith { iconPackR.drawable.ic_excel_medium_solid }

private val executableMap = arrayOf(
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
    iconPackR.drawable.ic_executable_medium_solid
}

private val webLandMap =
    arrayOf("as", "asc", "ascs").associateWith { iconPackR.drawable.ic_web_lang_medium_solid }

private val fontMap =
    arrayOf("fnt", "fon", "otf", "ttf").associateWith { iconPackR.drawable.ic_font_medium_solid }


private val illustratorMap = arrayOf(
    "ai",
    "aia",
    "aip",
    "ait",
    "art",
    "irs"
).associateWith { iconPackR.drawable.ic_illustrator_medium_solid }

private val imageMap = arrayOf(
    "jpg",
    "jpeg",
    "tga",
    "tif",
    "tiff",
    "bmp",
    "gif",
    "png"
).associateWith { iconPackR.drawable.ic_image_medium_solid }

private val indesignMap = mutableMapOf(Pair("indd", iconPackR.drawable.ic_indesign_medium_solid))


private val pfgMap = mutableMapOf(Pair("pdf", iconPackR.drawable.ic_pdf_medium_solid))

private val photoShopMap = arrayOf(
    "abr",
    "csh",
    "psb",
    "psd"
).associateWith { iconPackR.drawable.ic_photoshop_medium_solid }

private val powerPointMap = arrayOf(
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
).associateWith { iconPackR.drawable.ic_powerpoint_medium_solid }

private val premiereMap = arrayOf(
    "plb",
    "ppj",
    "prproj",
    "prtpset"
).associateWith { iconPackR.drawable.ic_premiere_medium_solid }

private val rawMap = arrayOf(
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
).associateWith { iconPackR.drawable.ic_raw_medium_solid }

private val spreadShitMap = arrayOf(
    "123",
    "gsheet",
    "nb",
    "ods",
    "ots",
    "sxc",
    "xlr"
).associateWith { iconPackR.drawable.ic_spreadsheet_medium_solid }


private val torrentMap =
    mutableMapOf(Pair("torrent", iconPackR.drawable.ic_torrent_medium_solid))

private val vectorMap = arrayOf(
    "cdr",
    "eps",
    "ps",
    "svg",
    "svgz"
).associateWith { iconPackR.drawable.ic_vector_medium_solid }

private val videoMap = arrayOf(
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
).associateWith { iconPackR.drawable.ic_video_medium_solid }


private val webDataMap = arrayOf(
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
).associateWith { iconPackR.drawable.ic_web_data_medium_solid }

private val worldMap = arrayOf(
    "doc",
    "docm",
    "docx",
    "dot",
    "dotx",
    "wps"
).associateWith { iconPackR.drawable.ic_word_medium_solid }

private val pagesMap = mutableMapOf(Pair("pages", iconPackR.drawable.ic_pages_medium_solid))

private val xdMap =
    mutableMapOf(Pair("Xd", iconPackR.drawable.ic_experiencedesign_medium_solid))

private val keyNodeMap =
    mutableMapOf(Pair("key", iconPackR.drawable.ic_keynote_medium_solid))

private val numbersMap =
    mutableMapOf(Pair("numbers", iconPackR.drawable.ic_numbers_medium_solid))

private val urlMap = mutableMapOf(Pair("url", iconPackR.drawable.ic_url_medium_solid))

private val openFileMap =
    arrayOf("odp", "odt", "ods").associateWith { iconPackR.drawable.ic_openoffice_medium_solid }

private val mergedMap =
    textExtensionMap + webDataMap + threeDMap + audioMap + cadMap + compressedMap +
            executableMap + excelMap + dmgMap + webLandMap +
            fontMap + illustratorMap + imageMap +
            indesignMap + pfgMap + photoShopMap +
            powerPointMap + premiereMap + rawMap + spreadShitMap + torrentMap +
            vectorMap + worldMap + pagesMap + xdMap + keyNodeMap +
            numbersMap + urlMap + openFileMap + afterEffectsMap + videoMap

private val treeMap = TreeMap<String, Int>(String.CASE_INSENSITIVE_ORDER).apply {
    putAll(mergedMap)
}
