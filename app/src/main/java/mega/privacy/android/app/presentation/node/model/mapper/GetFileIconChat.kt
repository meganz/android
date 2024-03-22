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
private fun getFileIconChat(extension: String) = when (extension) {
    "3ds", "3dm", "max", "obj" -> R.drawable.ic_3d_medium_solid
    "aec", "aep", "aepx", "aes", "aet", "aetx" -> R.drawable.ic_aftereffects_medium_solid
    "aif", "aiff", "wav", "flac", "iff", "m4a", "wma", "oga", "ogg", "mp3", "3ga", "opus", "weba", "ra", "ram", "rm" -> R.drawable.ic_audio_medium_solid
    "dwg", "dxf" -> R.drawable.ic_cad_medium_solid
    "bz2", "gz", "rar", "tar", "tbz", "tgz", "zip", "deb", "udeb", "rpm", "air", "7z", "bz", "bzip2", "cab", "lha", "gzip", "ace", "arc", "pkg" -> R.drawable.ic_compressed_medium_solid
    "dmg" -> R.drawable.ic_dmg_medium_solid
    "xla", "xlam", "xll", "xlm", "xls", "xlsm", "xlsx", "xlt", "xltm", "xltx" -> R.drawable.ic_excel_medium_solid
    "apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf" -> R.drawable.ic_executable_medium_solid
    "as", "asc", "ascs" -> R.drawable.ic_web_lang_medium_solid
    "fnt", "fon", "otf", "ttf" -> R.drawable.ic_font_medium_solid
    "ai", "aia", "aip", "ait", "art", "irs" -> R.drawable.ic_illustrator_medium_solid
    "jpg", "jpeg", "tga", "tif", "tiff", "bmp", "gif", "png" -> R.drawable.ic_image_medium_solid
    "indd" -> R.drawable.ic_indesign_medium_solid
    "pdf" -> R.drawable.ic_pdf_medium_solid
    "abr", "csh", "psb", "psd" -> R.drawable.ic_photoshop_medium_solid
    "pot", "potm", "potx", "ppam", "ppc", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx" -> R.drawable.ic_powerpoint_medium_solid
    "plb", "ppj", "prproj", "prtpset" -> R.drawable.ic_premiere_medium_solid
    "3fr", "mef", "arw", "bay", "cr2", "dcr", "dng", "erf", "fff", "mrw", "nef", "orf", "pef", "rw2", "rwl", "srf" -> R.drawable.ic_raw_medium_solid
    "123", "gsheet", "nb", "ots", "sxc", "xlr" -> R.drawable.ic_spreadsheet_medium_solid
    "ans", "ascii", "log", "rtf", "txt", "wpd" -> R.drawable.ic_text_medium_solid
    "torrent" -> R.drawable.ic_torrent_medium_solid
    "cdr", "eps", "ps", "svg", "svgz" -> R.drawable.ic_vector_medium_solid
    "3g2", "3gp", "asf", "avi", "mkv", "mov", "mpeg", "mpg", "wmv", "3gpp", "h261", "h263", "h264", "jpgv", "jpm", "jpgm", "mp4", "mp4v", "mpg4", "mpe", "m1v", "m2v", "ogv", "qt", "m4u", "webm", "f4v", "fli", "m4v", "mk3d", "movie", "vob" -> R.drawable.ic_video_medium_solid
    "asp", "aspx", "php", "php3", "php4", "php5", "phtml", "css", "inc", "js", "xml" -> R.drawable.ic_web_data_medium_solid
    "doc", "docm", "docx", "dot", "dotx", "wps" -> R.drawable.ic_word_medium_solid
    "pages" -> R.drawable.ic_pages_medium_solid
    "Xd" -> R.drawable.ic_experiencedesign_medium_solid
    "key" -> R.drawable.ic_keynote_medium_solid
    "numbers" -> R.drawable.ic_numbers_medium_solid
    "odp", "odt", "ods" -> R.drawable.ic_openoffice_medium_solid
    "url" -> R.drawable.ic_url_medium_solid
    else -> R.drawable.ic_generic_medium_solid
}