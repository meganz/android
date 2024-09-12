package mega.privacy.android.core.ui.mapper

import mega.privacy.android.icon.pack.R as iconPackR
import java.util.TreeMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper which will return icon for FileTypeInfo
 */
@Singleton
class FileTypeIconMapper @Inject constructor() {
    /**
     * invoke
     * @param fileExtension [String]
     * @param iconType [IconType]
     */
    operator fun invoke(fileExtension: String, iconType: IconType = IconType.Solid): Int =
        treeMap[fileExtension]?.getOrNull(iconType.index)
            ?: iconPackR.drawable.ic_generic_medium_solid
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
    ).associateWith {
        listOf(
            iconPackR.drawable.ic_text_medium_solid,
            iconPackR.drawable.ic_text_thumbnail_outline
        )
    }


private val threeDMap =
    arrayOf("3ds", "3dm", "max", "obj").associateWith {
        listOf(
            iconPackR.drawable.ic_3d_medium_solid,
            iconPackR.drawable.ic_3d_thumbnail_outline
        )
    }

private val afterEffectsMap =
    arrayOf(
        "aec",
        "aep",
        "aepx",
        "aes",
        "aet",
        "aetx"
    ).associateWith {
        listOf(
            iconPackR.drawable.ic_aftereffects_medium_solid,
            iconPackR.drawable.ic_aftereffects_thumbnail_outline
        )
    }


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
    ).associateWith {
        listOf(
            iconPackR.drawable.ic_audio_medium_solid,
            iconPackR.drawable.ic_audio_thumbnail_outline
        )
    }

private val cadMap = arrayOf("dwg", "dxf").associateWith {
    listOf(
        iconPackR.drawable.ic_cad_medium_solid,
        iconPackR.drawable.ic_cad_thumbnail_outline
    )
}

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
).associateWith {
    listOf(
        iconPackR.drawable.ic_compressed_medium_solid,
        iconPackR.drawable.ic_compressed_thumbnail_outline
    )
}


private val dmgMap = mutableMapOf(
    Pair(
        "dmg",
        listOf(
            iconPackR.drawable.ic_dmg_medium_solid,
            iconPackR.drawable.ic_dmg_thumbnail_outline
        )
    )
)


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
).associateWith {
    listOf(
        iconPackR.drawable.ic_excel_medium_solid,
        iconPackR.drawable.ic_excel_thumbnail_outline
    )
}

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
    listOf(
        iconPackR.drawable.ic_executable_medium_solid,
        iconPackR.drawable.ic_executable_thumbnail_outline
    )
}

private val webLandMap =
    arrayOf("as", "asc", "ascs").associateWith {
        listOf(
            iconPackR.drawable.ic_web_lang_medium_solid,
            iconPackR.drawable.ic_web_lang_thumbnail_outline
        )
    }

private val fontMap =
    arrayOf("fnt", "fon", "otf", "ttf").associateWith {
        listOf(
            iconPackR.drawable.ic_font_medium_solid,
            iconPackR.drawable.ic_font_thumbnail_outline
        )
    }


private val illustratorMap = arrayOf(
    "ai",
    "aia",
    "aip",
    "ait",
    "art",
    "irs"
).associateWith {
    listOf(
        iconPackR.drawable.ic_illustrator_medium_solid,
        iconPackR.drawable.ic_illustrator_thumbnail_outline
    )
}

private val imageMap = arrayOf(
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
        iconPackR.drawable.ic_image_medium_solid,
        iconPackR.drawable.ic_image_thumbnail_outline
    )
}

private val indesignMap = mutableMapOf(
    Pair(
        "indd",
        listOf(
            iconPackR.drawable.ic_indesign_medium_solid,
            iconPackR.drawable.ic_indesign_thumbnail_outline
        )
    )
)


private val pdfMap = mutableMapOf(
    Pair(
        "pdf",
        listOf(
            iconPackR.drawable.ic_pdf_medium_solid,
            iconPackR.drawable.ic_pdf_thumbnail_outline
        )
    )
)

private val photoShopMap = arrayOf(
    "abr",
    "csh",
    "psb",
    "psd"
).associateWith {
    listOf(
        iconPackR.drawable.ic_photoshop_medium_solid,
        iconPackR.drawable.ic_photoshop_thumbnail_outline
    )
}

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
).associateWith {
    listOf(
        iconPackR.drawable.ic_powerpoint_medium_solid,
        iconPackR.drawable.ic_powerpoint_thumbnail_outline
    )
}

private val premiereMap = arrayOf(
    "plb",
    "ppj",
    "prproj",
    "prtpset"
).associateWith {
    listOf(
        iconPackR.drawable.ic_premiere_medium_solid,
        iconPackR.drawable.ic_premiere_thumbnail_outline
    )
}

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
).associateWith {
    listOf(
        iconPackR.drawable.ic_raw_medium_solid,
        iconPackR.drawable.ic_raw_thumbnail_outline
    )
}

private val spreadShitMap = arrayOf(
    "123",
    "gsheet",
    "nb",
    "ods",
    "ots",
    "sxc",
    "xlr"
).associateWith {
    listOf(
        iconPackR.drawable.ic_spreadsheet_medium_solid,
        iconPackR.drawable.ic_spreadsheet_thumbnail_outline
    )
}


private val torrentMap =
    mutableMapOf(
        Pair(
            "torrent",
            listOf(
                iconPackR.drawable.ic_torrent_medium_solid,
                iconPackR.drawable.ic_torrent_thumbnail_outline
            )
        )
    )

private val vectorMap = arrayOf(
    "cdr",
    "eps",
    "ps",
    "svg",
    "svgz"
).associateWith {
    listOf(
        iconPackR.drawable.ic_vector_medium_solid,
        iconPackR.drawable.ic_vector_thumbnail_outline
    )
}

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
).associateWith {
    listOf(
        iconPackR.drawable.ic_video_medium_solid,
        iconPackR.drawable.ic_video_thumbnail_outline
    )
}


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
).associateWith {
    listOf(
        iconPackR.drawable.ic_web_data_medium_solid,
        iconPackR.drawable.ic_web_data_thumbnail_outline
    )
}

private val worldMap = arrayOf(
    "doc",
    "docm",
    "docx",
    "dot",
    "dotx",
    "wps"
).associateWith {
    listOf(
        iconPackR.drawable.ic_word_medium_solid,
        iconPackR.drawable.ic_word_thumbnail_outline
    )
}

private val pagesMap = mutableMapOf(
    Pair(
        "pages",
        listOf(
            iconPackR.drawable.ic_pages_medium_solid,
            iconPackR.drawable.ic_pages_thumbnail_outline
        )
    )
)

private val xdMap =
    mutableMapOf(
        Pair(
            "Xd",
            listOf(
                iconPackR.drawable.ic_experiencedesign_medium_solid,
                iconPackR.drawable.ic_experiencedesign_thumbnail_outline
            )
        )
    )

private val keyNodeMap =
    mutableMapOf(
        Pair(
            "key",
            listOf(
                iconPackR.drawable.ic_keynote_medium_solid,
                iconPackR.drawable.ic_keynote_thumbnail_outline
            )
        )
    )

private val numbersMap =
    mutableMapOf(
        Pair(
            "numbers",
            listOf(
                iconPackR.drawable.ic_numbers_medium_solid,
                iconPackR.drawable.ic_numbers_thumbnail_outline
            )
        )
    )

private val urlMap = mutableMapOf(
    Pair(
        "url",
        listOf(
            iconPackR.drawable.ic_url_medium_solid,
            iconPackR.drawable.ic_url_thumbnail_outline
        )
    )
)

private val openFileMap =
    arrayOf("odp", "odt", "ods").associateWith {
        listOf(
            iconPackR.drawable.ic_openoffice_medium_solid,
            iconPackR.drawable.ic_openoffice_thumbnail_outline
        )
    }

private val mergedMap =
    textExtensionMap + webDataMap + threeDMap + audioMap + cadMap + compressedMap +
            executableMap + excelMap + dmgMap + webLandMap +
            fontMap + illustratorMap + imageMap +
            indesignMap + pdfMap + photoShopMap +
            powerPointMap + premiereMap + rawMap + spreadShitMap + torrentMap +
            vectorMap + worldMap + pagesMap + xdMap + keyNodeMap +
            numbersMap + urlMap + openFileMap + afterEffectsMap + videoMap

private val treeMap = TreeMap<String, List<Int>>(String.CASE_INSENSITIVE_ORDER).apply {
    putAll(mergedMap)
}


/**
 * Type of icon
 *
 * @property index index of type
 */
enum class IconType(val index: Int) {

    /**
     * Solid icon
     */
    Solid(0),

    /**
     * Outline icon
     */
    Outlined(1),
}