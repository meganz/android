package mega.privacy.android.app.presentation.node.model.mapper

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.annotation.DrawableRes
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
        "3ds", "3dm", "max", "obj" -> IconPackR.drawable.ic_3d_thumbnail_outline
        "aec", "aep", "aepx", "aes", "aet", "aetx" -> IconPackR.drawable.ic_aftereffects_thumbnail_outline
        "aif", "aiff", "wav", "flac", "iff", "m4a", "wma", "oga", "ogg", "mp3", "3ga", "opus", "weba", "ra", "ram", "rm" -> IconPackR.drawable.ic_audio_thumbnail_outline
        "dwg", "dxf" -> IconPackR.drawable.ic_cad_thumbnail_outline
        "bz2", "gz", "rar", "tar", "tbz", "tgz", "zip", "deb", "udeb", "rpm", "air", "7z", "bz", "bzip2", "cab", "lha", "gzip", "ace", "arc", "pkg" -> IconPackR.drawable.ic_compressed_thumbnail_outline
        "dmg" -> IconPackR.drawable.ic_dmg_thumbnail_outline
        "xla", "xlam", "xll", "xlm", "xls", "xlsm", "xlsx", "xlt", "xltm", "xltx" -> IconPackR.drawable.ic_excel_thumbnail_outline
        "apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf" -> IconPackR.drawable.ic_executabe_thumbnail_outline
        "as", "asc", "ascs" -> IconPackR.drawable.ic_web_lang_thumbnail_outline
        "fnt", "fon", "otf", "ttf" -> IconPackR.drawable.ic_font_thumbnail_outline
        "ai", "aia", "aip", "ait", "art", "irs" -> IconPackR.drawable.ic_illustrator_thumbnail_outline
        "jpg", "jpeg", "tga", "tif", "tiff", "bmp", "gif", "png" -> IconPackR.drawable.ic_image_thumbnail_outline
        "indd" -> IconPackR.drawable.ic_indesign_thumbnail_outline
        "pdf" -> IconPackR.drawable.ic_pdf_thumbnail_outline
        "abr", "csh", "psb", "psd" -> IconPackR.drawable.ic_photoshop_thumbnail_outline
        "pot", "potm", "potx", "ppam", "ppc", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx" -> IconPackR.drawable.ic_powerpoint_thumbnail_outline
        "plb", "ppj", "prproj", "prtpset" -> IconPackR.drawable.ic_premiere_thumbnail_outline
        "3fr", "mef", "arw", "bay", "cr2", "dcr", "dng", "erf", "fff", "mrw", "nef", "orf", "pef", "rw2", "rwl", "srf" -> IconPackR.drawable.ic_raw_thumbnail_outline
        "123", "gsheet", "nb", "ots", "sxc", "xlr" -> IconPackR.drawable.ic_spreadsheet_thumbnail_outline
        "ans", "ascii", "log", "rtf", "txt", "wpd" -> IconPackR.drawable.ic_text_thumbnail_outline
        "torrent" -> IconPackR.drawable.ic_torrent_thumbnail_outline
        "cdr", "eps", "ps", "svg", "svgz" -> IconPackR.drawable.ic_vector_thumbnail_outline
        "3g2", "3gp", "asf", "avi", "mkv", "mov", "mpeg", "mpg", "wmv", "3gpp", "h261", "h263", "h264", "jpgv", "jpm", "jpgm", "mp4", "mp4v", "mpg4", "mpe", "m1v", "m2v", "ogv", "qt", "m4u", "webm", "f4v", "fli", "m4v", "mk3d", "movie", "vob" -> IconPackR.drawable.ic_video_thumbnail_outline
        "asp", "aspx", "php", "php3", "php4", "php5", "phtml", "css", "inc", "js", "xml" -> IconPackR.drawable.ic_web_data_thumbnail_outline
        "doc", "docm", "docx", "dot", "dotx", "wps" -> IconPackR.drawable.ic_word_thumbnail_outline
        "pages" -> IconPackR.drawable.ic_pages_thumbnail_outline
        "Xd" -> IconPackR.drawable.ic_experiencedesign_thumbnail_outline
        "key" -> IconPackR.drawable.ic_keynote_thumbnail_outline
        "numbers" -> IconPackR.drawable.ic_numbers_thumbnail_outline
        "odp", "odt", "ods" -> IconPackR.drawable.ic_openoffice_thumbnail_outline
        "url" -> IconPackR.drawable.ic_url_thumbnail_outline
        else -> IconPackR.drawable.ic_generic_thumbnail_outline
    }