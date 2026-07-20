package mega.privacy.android.shared.nodes.mapper

import java.util.TreeMap

/**
 * File-type categories. The shared source of truth for [FileTypeIconMapper] (extension → icon) and
 * [FileTypeNameMapper] (extension → display name), so a file's icon and its type name always agree.
 * The extension → category mapping lives in [categoryByExtension].
 */
enum class FileTypeCategory {
    Text,
    WebData,
    ThreeD,
    Audio,
    Cad,
    Compressed,
    Executable,
    Excel,
    Dmg,
    WebLang,
    Font,
    Illustrator,
    Image,
    InDesign,
    Pdf,
    Photoshop,
    PowerPoint,
    Premiere,
    Raw,
    Spreadsheet,
    Torrent,
    Vector,
    Word,
    Pages,
    Xd,
    Keynote,
    Numbers,
    Url,
    OpenOffice,
    AfterEffects,
    Video,
    ;

    companion object {
        /**
         * The category [extension] belongs to (case-insensitive), or null if unrecognized.
         */
        fun fromExtension(extension: String): FileTypeCategory? = categoryByExtension[extension]
    }
}

/**
 * Extension → category lookup, built once and read allocation-free thereafter. Case-insensitive; on
 * overlap the category listed later wins (e.g. "apk" → [FileTypeCategory.Executable], "dmg" →
 * [FileTypeCategory.Dmg]), matching the historical icon-mapper merge order. The grouped table below
 * is a local temporary, so each extension string is retained only once (in the map).
 */
private val categoryByExtension: Map<String, FileTypeCategory> =
    TreeMap<String, FileTypeCategory>(String.CASE_INSENSITIVE_ORDER).apply {
        listOf(
            FileTypeCategory.Text to listOf("txt", "ans", "ascii", "log", "wpd", "json", "rtf"),
            FileTypeCategory.WebData to listOf(
                "asp", "aspx", "php", "php3", "php4", "php5", "phtml", "css", "inc", "js", "xml",
            ),
            FileTypeCategory.ThreeD to listOf("3ds", "3dm", "max", "obj"),
            FileTypeCategory.Audio to listOf(
                "aif", "aiff", "wav", "flac", "iff", "m4a", "wma", "oga", "ogg", "mp3", "3ga",
                "opus", "weba", "ra", "ram", "rm",
            ),
            FileTypeCategory.Cad to listOf("dwg", "dxf"),
            FileTypeCategory.Compressed to listOf(
                "bz2", "gz", "rar", "tar", "tbz", "tgz", "zip", "deb", "udeb", "rpm", "air", "apk",
                "dmg", "7z", "bz", "bzip2", "cab", "lha", "gzip", "ace", "arc", "pkg",
            ),
            FileTypeCategory.Executable to listOf(
                "apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf",
            ),
            FileTypeCategory.Excel to listOf(
                "xla", "xlam", "xll", "xlm", "xls", "xlsm", "xlsx", "xlt", "xltm", "xltx",
            ),
            FileTypeCategory.Dmg to listOf("dmg"),
            FileTypeCategory.WebLang to listOf("as", "asc", "ascs"),
            FileTypeCategory.Font to listOf("fnt", "fon", "otf", "ttf"),
            FileTypeCategory.Illustrator to listOf("ai", "aia", "aip", "ait", "art", "irs"),
            FileTypeCategory.Image to listOf(
                "jpg", "jpeg", "tga", "tif", "tiff", "bmp", "gif", "png", "heic", "webp",
            ),
            FileTypeCategory.InDesign to listOf("indd"),
            FileTypeCategory.Pdf to listOf("pdf"),
            FileTypeCategory.Photoshop to listOf("abr", "csh", "psb", "psd"),
            FileTypeCategory.PowerPoint to listOf(
                "pot", "potm", "potx", "ppam", "ppc", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx",
            ),
            FileTypeCategory.Premiere to listOf("plb", "ppj", "prproj", "prtpset"),
            FileTypeCategory.Raw to listOf(
                "3fr", "mef", "arw", "bay", "cr2", "dcr", "dng", "erf", "fff", "mrw", "nef", "orf",
                "pef", "rw2", "rwl", "srf", "iiq", "k25", "kdc", "mos", "raw", "sr2", "x3f", "cr3",
                "ciff",
            ),
            FileTypeCategory.Spreadsheet to listOf("123", "gsheet", "nb", "ods", "ots", "sxc", "xlr"),
            FileTypeCategory.Torrent to listOf("torrent"),
            FileTypeCategory.Vector to listOf("cdr", "eps", "ps", "svg", "svgz"),
            FileTypeCategory.Word to listOf("doc", "docm", "docx", "dot", "dotx", "wps"),
            FileTypeCategory.Pages to listOf("pages"),
            FileTypeCategory.Xd to listOf("Xd"),
            FileTypeCategory.Keynote to listOf("key"),
            FileTypeCategory.Numbers to listOf("numbers"),
            FileTypeCategory.Url to listOf("url"),
            FileTypeCategory.OpenOffice to listOf("odp", "odt", "ods"),
            FileTypeCategory.AfterEffects to listOf("aec", "aep", "aepx", "aes", "aet", "aetx"),
            FileTypeCategory.Video to listOf(
                "3g2", "3gp", "asf", "avi", "mkv", "mov", "mpeg", "mpg", "wmv", "3gpp", "h261",
                "h263", "h264", "jpgv", "jpm", "jpgm", "mp4", "mp4v", "mpg4", "mpe", "m1v", "m2v",
                "ogv", "qt", "m4u", "webm", "f4v", "fli", "m4v", "mk3d", "movie", "vob", "ts",
            ),
        ).forEach { (category, extensions) -> extensions.forEach { put(it, category) } }
    }
