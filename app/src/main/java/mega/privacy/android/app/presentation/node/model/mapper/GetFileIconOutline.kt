package mega.privacy.android.app.presentation.node.model.mapper

import androidx.annotation.DrawableRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.TypedFileNode

private val cache = mutableMapOf<String, Int>()

@DrawableRes
internal fun getFileIconOutline(fileNode: TypedFileNode): Int {
    return cache.getOrPut(fileNode.type.extension) {
        getFileIconOutline(fileNode.type.extension)
    }
}

@DrawableRes
private fun getFileIconOutline(extension: String) =
    when (extension) {
        "3ds", "3dm", "max", "obj" -> R.drawable.ic_3d_thumbnail_outline
        "aec", "aep", "aepx", "aes", "aet", "aetx" -> R.drawable.ic_aftereffects_thumbnail_outline
        "aif", "aiff", "wav", "flac", "iff", "m4a", "wma", "oga", "ogg", "mp3", "3ga", "opus", "weba" -> R.drawable.ic_audio_thumbnail_outline
        "dwg", "dxf" -> R.drawable.ic_cad_thumbnail_outline
        "bz2", "gz", "rar", "tar", "tbz", "tgz", "zip", "deb", "udeb", "rpm", "air", "7z", "bz", "bzip2", "cab", "lha", "gzip", "ace", "arc", "pkg" -> R.drawable.ic_compressed_thumbnail_outline
        "dmg" -> R.drawable.ic_dmg_thumbnail_outline
        "xla", "xlam", "xll", "xlm", "xls", "xlsm", "xlsx", "xlt", "xltm", "xltx" -> R.drawable.ic_excel_thumbnail_outline
        "apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf" -> R.drawable.ic_executabe_thumbnail_outline
        "as", "asc", "ascs" -> R.drawable.ic_web_lang_thumbnail_outline
        "fnt", "fon", "otf", "ttf" -> R.drawable.ic_font_thumbnail_outline
        "ai", "aia", "aip", "ait", "art", "irs" -> R.drawable.ic_illustrator_thumbnail_outline
        "jpg", "jpeg", "tga", "tif", "tiff", "bmp", "gif", "png" -> R.drawable.ic_image_thumbnail_outline
        "indd" -> R.drawable.ic_indesign_thumbnail_outline
        "pdf" -> R.drawable.ic_pdf_thumbnail_outline
        "abr", "csh", "psb", "psd" -> R.drawable.ic_photoshop_thumbnail_outline
        "pot", "potm", "potx", "ppam", "ppc", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx" -> R.drawable.ic_powerpoint_thumbnail_outline
        "plb", "ppj", "prproj", "prtpset" -> R.drawable.ic_premiere_thumbnail_outline
        "3fr", "mef", "arw", "bay", "cr2", "dcr", "dng", "erf", "fff", "mrw", "nef", "orf", "pef", "rw2", "rwl", "srf" -> R.drawable.ic_raw_thumbnail_outline
        "123", "gsheet", "nb", "ots", "sxc", "xlr" -> R.drawable.ic_spreadsheet_thumbnail_outline
        "ans", "ascii", "log", "rtf", "txt", "wpd" -> R.drawable.ic_text_thumbnail_outline
        "torrent" -> R.drawable.ic_torrent_thumbnail_outline
        "cdr", "eps", "ps", "svg", "svgz" -> R.drawable.ic_vector_thumbnail_outline
        "3g2", "3gp", "asf", "avi", "mkv", "mov", "mpeg", "mpg", "wmv", "3gpp", "h261", "h263", "h264", "jpgv", "jpm", "jpgm", "mp4", "mp4v", "mpg4", "mpe", "m1v", "m2v", "ogv", "qt", "m4u", "webm", "f4v", "fli", "m4v", "mk3d", "movie" -> R.drawable.ic_video_thumbnail_outline
        "asp", "aspx", "php", "php3", "php4", "php5", "phtml", "css", "inc", "js", "xml" -> R.drawable.ic_web_data_thumbnail_outline
        "doc", "docm", "docx", "dot", "dotx", "wps" -> R.drawable.ic_word_thumbnail_outline
        "pages" -> R.drawable.ic_pages_thumbnail_outline
        "Xd" -> R.drawable.ic_experiencedesign_thumbnail_outline
        "key" -> R.drawable.ic_keynote_thumbnail_outline
        "numbers" -> R.drawable.ic_numbers_thumbnail_outline
        "odp", "odt", "ods" -> R.drawable.ic_openoffice_thumbnail_outline
        "url" -> R.drawable.ic_url_thumbnail_outline
        else -> R.drawable.ic_generic_thumbnail_outline
    }