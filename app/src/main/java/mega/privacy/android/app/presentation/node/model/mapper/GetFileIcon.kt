package mega.privacy.android.app.presentation.node.model.mapper

import mega.privacy.android.core.R as CoreUiR
import androidx.annotation.DrawableRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.TypedFileNode

private val cache = mutableMapOf<String, Int>()

@DrawableRes
internal fun getFileIcon(fileNode: TypedFileNode): Int {
    val extension = fileNode.name.substringAfterLast(
        delimiter = ".",
        missingDelimiterValue = ""
    )
    return cache.getOrElse(extension) {
        getFileIcon(extension).also {
            cache[extension] = it
        }
    }
}

@DrawableRes
private fun getFileIcon(extension: String) =
    when (extension) {
        "3ds", "3dm", "max", "obj" -> R.drawable.ic_3d_thumbnail
        "aec", "aep", "aepx", "aes", "aet", "aetx" -> R.drawable.ic_aftereffects_thumbnail
        "aif", "aiff", "wav", "flac", "iff", "m4a", "wma", "oga", "ogg", "mp3", "3ga", "opus", "weba" -> R.drawable.ic_audio_thumbnail
        "dwg", "dxf" -> R.drawable.ic_cad_thumbnail
        "bz2", "gz", "rar", "tar", "tbz", "tgz", "zip", "deb", "udeb", "rpm", "air", "7z", "bz", "bzip2", "cab", "lha", "gzip", "ace", "arc", "pkg" -> R.drawable.ic_compressed_thumbnail
        "accdb", "db", "dbf", "mdb", "pdb", "sql" -> R.drawable.database_thumbnail
        "dmg" -> R.drawable.ic_dmg_thumbnail
        "dwt" -> R.drawable.dreamweaver_thumbnail
        "xla", "xlam", "xll", "xlm", "xls", "xlsm", "xlsx", "xlt", "xltm", "xltx" -> R.drawable.ic_excel_thumbnail
        "apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf" -> R.drawable.ic_executabe_thumbnail
        "as", "asc", "ascs" -> R.drawable.ic_web_lang_thumbnail
        "fla" -> R.drawable.flash_thumbnail
        "fnt", "fon", "otf", "ttf" -> R.drawable.ic_font_thumbnail
        "gpx", "kml", "kmz" -> R.drawable.gis_thumbnail
        "dhtml", "htm", "html", "shtml", "xhtml" -> R.drawable.html_thumbnail
        "ai", "aia", "aip", "ait", "art", "irs" -> R.drawable.ic_illustrator_thumbnail
        "jpg", "jpeg", "tga", "tif", "tiff", "bmp", "gif", "png" -> CoreUiR.drawable.ic_image_thumbnail
        "indd" -> R.drawable.ic_indesign_thumbnail
        "class", "jar", "java" -> R.drawable.java_thumbnail
        "mid", "midi" -> R.drawable.midi_thumbnail
        "pdf" -> R.drawable.ic_pdf_thumbnail
        "abr", "csh", "psb", "psd" -> R.drawable.ic_photoshop_thumbnail
        "asx", "m3u", "pls" -> R.drawable.playlist_thumbnail
        "pcast" -> R.drawable.podcast_thumbnail
        "pot", "potm", "potx", "ppam", "ppc", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx" -> R.drawable.ic_powerpoint_thumbnail
        "plb", "ppj", "prproj", "prtpset" -> R.drawable.ic_premiere_thumbnail
        "3fr", "mef", "arw", "bay", "cr2", "dcr", "dng", "erf", "fff", "mrw", "nef", "orf", "pef", "rw2", "rwl", "srf" -> R.drawable.ic_raw_thumbnail
        "ra", "ram", "rm" -> R.drawable.real_audio_thumbnail
        "c", "cc", "cgi", "cpp", "cxx", "dll", "h", "hpp", "pl", "py", "sh" -> R.drawable.source_thumbnail
        "123", "gsheet", "nb", "ots", "sxc", "xlr" -> R.drawable.ic_spreadsheet_thumbnail
        "srt" -> R.drawable.subtitles_thumbnail
        "swf", "flv" -> R.drawable.swf_thumbnail
        "ans", "ascii", "log", "rtf", "txt", "wpd" -> R.drawable.ic_text_thumbnail
        "torrent" -> R.drawable.ic_torrent_thumbnail
        "vcard", "vcf" -> R.drawable.vcard_thumbnail
        "cdr", "eps", "ps", "svg", "svgz" -> R.drawable.ic_vector_thumbnail
        "3g2", "3gp", "asf", "avi", "mkv", "mov", "mpeg", "mpg", "wmv", "3gpp", "h261", "h263", "h264", "jpgv", "jpm", "jpgm", "mp4", "mp4v", "mpg4", "mpe", "m1v", "m2v", "ogv", "qt", "m4u", "webm", "f4v", "fli", "m4v", "mk3d", "movie" -> R.drawable.ic_video_thumbnail
        "vob" -> R.drawable.video_vob_thumbnail
        "asp", "aspx", "php", "php3", "php4", "php5", "phtml", "css", "inc", "js", "xml" -> R.drawable.ic_web_data_thumbnail
        "doc", "docm", "docx", "dot", "dotx", "wps" -> R.drawable.ic_word_thumbnail
        "pages" -> R.drawable.ic_pages_thumbnail
        "Xd" -> R.drawable.ic_experiencedesign_thumbnail
        "key" -> R.drawable.ic_keynote_thumbnail
        "numbers" -> R.drawable.ic_numbers_thumbnail
        "odp", "odt", "ods" -> R.drawable.ic_openoffice_thumbnail
        "sketch" -> R.drawable.ic_sketch_thumbnail
        "url" -> R.drawable.ic_url_thumbnail
        else -> R.drawable.ic_generic_thumbnail
    }