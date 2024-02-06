package mega.privacy.android.app.presentation.node.model.mapper

import androidx.annotation.DrawableRes
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.icon.pack.R

private val cache = mutableMapOf<String, Int>()

@DrawableRes
internal fun getFileIconChat(fileTypeInfo: FileTypeInfo): Int {
    return cache.getOrPut(fileTypeInfo.extension) {
        getFileIconChat(fileTypeInfo.extension)
    }
}

@DrawableRes
private fun getFileIconChat(extension: String) =
    when (extension) {
        "3ds", "3dm", "max", "obj" -> R.drawable.ic_3d
        "aec", "aep", "aepx", "aes", "aet", "aetx" -> R.drawable.ic_aftereffects
        "aif", "aiff", "wav", "flac", "iff", "m4a", "wma", "oga", "ogg", "mp3", "3ga", "opus", "weba" -> R.drawable.ic_audio
        "dwg", "dxf" -> R.drawable.ic_cad
        "bz2", "gz", "rar", "tar", "tbz", "tgz", "zip", "deb", "udeb", "rpm", "air", "7z", "bz", "bzip2", "cab", "lha", "gzip", "ace", "arc", "pkg" -> R.drawable.ic_compressed
        "dmg" -> R.drawable.ic_dmg
        "xla", "xlam", "xll", "xlm", "xls", "xlsm", "xlsx", "xlt", "xltm", "xltx" -> R.drawable.ic_excel
        "apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf" -> R.drawable.ic_executable
        "as", "asc", "ascs" -> R.drawable.ic_web_lang
        "fnt", "fon", "otf", "ttf" -> R.drawable.ic_font
        "ai", "aia", "aip", "ait", "art", "irs" -> R.drawable.ic_illustrator
        "jpg", "jpeg", "tga", "tif", "tiff", "bmp", "gif", "png" -> R.drawable.ic_image
        "indd" -> R.drawable.ic_indesign
        "pdf" -> R.drawable.ic_pdf
        "abr", "csh", "psb", "psd" -> R.drawable.ic_photoshop
        "pot", "potm", "potx", "ppam", "ppc", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx" -> R.drawable.ic_powerpoint
        "plb", "ppj", "prproj", "prtpset" -> R.drawable.ic_premiere
        "3fr", "mef", "arw", "bay", "cr2", "dcr", "dng", "erf", "fff", "mrw", "nef", "orf", "pef", "rw2", "rwl", "srf" -> R.drawable.ic_raw
        "123", "gsheet", "nb", "ots", "sxc", "xlr" -> R.drawable.ic_spreadsheet
        "ans", "ascii", "log", "rtf", "txt", "wpd" -> R.drawable.ic_text
        "torrent" -> R.drawable.ic_torrent
        "cdr", "eps", "ps", "svg", "svgz" -> R.drawable.ic_vector
        "3g2", "3gp", "asf", "avi", "mkv", "mov", "mpeg", "mpg", "wmv", "3gpp", "h261", "h263", "h264", "jpgv", "jpm", "jpgm", "mp4", "mp4v", "mpg4", "mpe", "m1v", "m2v", "ogv", "qt", "m4u", "webm", "f4v", "fli", "m4v", "mk3d", "movie" -> R.drawable.ic_video_file
        "asp", "aspx", "php", "php3", "php4", "php5", "phtml", "css", "inc", "js", "xml" -> R.drawable.ic_web_data
        "doc", "docm", "docx", "dot", "dotx", "wps" -> R.drawable.ic_word
        "pages" -> R.drawable.ic_pages
        "Xd" -> R.drawable.ic_experiencedesign
        "key" -> R.drawable.ic_keynote
        "numbers" -> R.drawable.ic_numbers
        "odp", "odt", "ods" -> R.drawable.ic_openoffice
        "url" -> R.drawable.ic_url
        else -> R.drawable.ic_generic
    }