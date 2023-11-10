package mega.privacy.android.feature.sync.ui.mapper

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.domain.entity.node.TypedFileNode
import javax.inject.Inject

/**
 * Mapper which will return icon for FileTypeInfo
 */
class FileTypeIconMapper @Inject constructor() {
    /**
     * invoke
     * @param typedFileNode [TypedFileNode]
     */
    operator fun invoke(typedFileNode: TypedFileNode): Int {
        return mergedMap[typedFileNode.type.extension] ?: iconPackR.drawable.ic_generic_list
    }
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
    ).associateWith { iconPackR.drawable.ic_text_list }


private val threeDMap =
    arrayOf("3ds", "3dm", "max", "obj").associateWith { iconPackR.drawable.ic_3d_list }

private val afterEffectsMap =
    arrayOf(
        "aec",
        "aep",
        "aepx",
        "aes",
        "aet",
        "aetx"
    ).associateWith { iconPackR.drawable.ic_aftereffects_list }


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
        "weba"
    ).associateWith { iconPackR.drawable.ic_audio_list }

private val cadMap = arrayOf("dwg", "dxf").associateWith { iconPackR.drawable.ic_cad_list }

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
).associateWith { iconPackR.drawable.ic_compressed_list }

private val databaseMap = arrayOf(
    "accdb",
    "db",
    "dbf",
    "mdb",
    "pdb",
    "sql"
).associateWith { iconPackR.drawable.ic_database_list }

private val dmgMap = mutableMapOf(Pair("dmg", iconPackR.drawable.ic_dmg_list))

private val dreamViewerMap =
    mutableMapOf(Pair("dwt", iconPackR.drawable.ic_dreamweaver_list))


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
).associateWith { iconPackR.drawable.ic_excel_list }

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
    iconPackR.drawable.ic_executable_list
}

private val webLandMap =
    arrayOf("as", "asc", "ascs").associateWith { iconPackR.drawable.ic_web_lang_list }

private val flashMap = mutableMapOf(Pair("flg", iconPackR.drawable.ic_flash_list))

private val fontMap =
    arrayOf("fnt", "fon", "otf", "ttf").associateWith { iconPackR.drawable.ic_font_list }

private val gisMap =
    arrayOf("gpx", "kml", "kmz").associateWith { iconPackR.drawable.ic_gis_list }

private val htmlMap = arrayOf(
    "dhtml",
    "htm",
    "html",
    "shtml",
    "xhtml"
).associateWith { iconPackR.drawable.ic_html_list }

private val illustratorMap = arrayOf(
    "ai",
    "aia",
    "aip",
    "ait",
    "art",
    "irs"
).associateWith { iconPackR.drawable.ic_illustrator_list }

private val imageMap = arrayOf(
    "jpg",
    "jpeg",
    "tga",
    "tif",
    "tiff",
    "bmp",
    "gif",
    "png"
).associateWith { iconPackR.drawable.ic_image_list }

private val indesignMap = mutableMapOf(Pair("indd", iconPackR.drawable.ic_indesign_list))

private val javaMap = arrayOf("class", "jar", "java").associateWith {
    iconPackR.drawable.ic_java_list
}

private val midiMap = arrayOf("mid", "midi").associateWith { iconPackR.drawable.ic_midi_list }

private val pfgMap = mutableMapOf(Pair("pdf", iconPackR.drawable.ic_pdf_list))

private val photoShopMap =
    arrayOf("abr", "csh", "psb", "psd").associateWith { iconPackR.drawable.ic_photoshop_list }

private val playListMap =
    arrayOf("asx", "m3u", "pls").associateWith { iconPackR.drawable.ic_playlist_list }

private val podCastMap =
    mutableMapOf(Pair("pcast", iconPackR.drawable.ic_podcast_list))

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
).associateWith { iconPackR.drawable.ic_powerpoint_list }

private val premiereMap = arrayOf(
    "plb",
    "ppj",
    "prproj",
    "prtpset"
).associateWith { iconPackR.drawable.ic_premiere_list }

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
).associateWith { iconPackR.drawable.ic_raw_list }

private val realAudioMap =
    arrayOf("ra", "ram", "rm").associateWith { iconPackR.drawable.ic_real_audio_list }

private val sourceListMap = arrayOf(
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
).associateWith { iconPackR.drawable.ic_source_list }

private val spreadShitMap = arrayOf(
    "123",
    "gsheet",
    "nb",
    "ods",
    "ots",
    "sxc",
    "xlr"
).associateWith { iconPackR.drawable.ic_spreadsheet_list }

private val subTitleMap = mutableMapOf(Pair("srt", iconPackR.drawable.ic_subtitles_list))

private val swfMap = arrayOf("swf", "flv").associateWith { iconPackR.drawable.ic_swf_list }

private val torrentMap =
    mutableMapOf(Pair("torrent", iconPackR.drawable.ic_torrent_list))

private val vCardMap =
    arrayOf("vcard", "vcf").associateWith { iconPackR.drawable.ic_vcard_list }

private val vectorMap = arrayOf(
    "cdr",
    "eps",
    "ps",
    "svg",
    "svgz"
).associateWith { iconPackR.drawable.ic_vector_list }

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
).associateWith { iconPackR.drawable.ic_video_list }

private val vobVideoMap =
    mutableMapOf(Pair("vob", iconPackR.drawable.ic_video_vob_list))

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
).associateWith { iconPackR.drawable.ic_web_data_list }

private val worldMap = arrayOf(
    "doc",
    "docm",
    "docx",
    "dot",
    "dotx",
    "wps"
).associateWith { iconPackR.drawable.ic_word_list }

private val pagesMap = mutableMapOf(Pair("pages", iconPackR.drawable.ic_pages_list))

private val xdMap =
    mutableMapOf(Pair("Xd", iconPackR.drawable.ic_experiencedesign_list))

private val keyNodeMap =
    mutableMapOf(Pair("key", iconPackR.drawable.ic_keynote_list))

private val numbersMap =
    mutableMapOf(Pair("numbers", iconPackR.drawable.ic_numbers_list))

private val sketchMap =
    mutableMapOf(Pair("sketch", iconPackR.drawable.ic_sketch_list))

private val urlMap = mutableMapOf(Pair("url", iconPackR.drawable.ic_url_list))

private val openFileMap =
    arrayOf("odp", "odt", "ods").associateWith { iconPackR.drawable.ic_openoffice_list }

private val mergedMap =
    textExtensionMap + webDataMap + threeDMap + audioMap + cadMap + compressedMap +
            databaseMap + executableMap + excelMap + dmgMap + dreamViewerMap + webLandMap +
            flashMap + fontMap + gisMap + htmlMap + illustratorMap + imageMap +
            indesignMap + javaMap + midiMap + pfgMap + photoShopMap + playListMap +
            podCastMap + powerPointMap + premiereMap + rawMap + realAudioMap +
            sourceListMap + spreadShitMap + subTitleMap + swfMap + torrentMap + vCardMap +
            vectorMap + vobVideoMap + worldMap + pagesMap + xdMap + keyNodeMap +
            numbersMap + sketchMap + urlMap + openFileMap + afterEffectsMap + videoMap

