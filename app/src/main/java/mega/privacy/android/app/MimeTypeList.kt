package mega.privacy.android.app

import android.net.Uri
import android.util.SparseArray
import android.webkit.MimeTypeMap
import mega.privacy.android.app.components.textFormatter.TextFormatterUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.getMimeType
import mega.privacy.android.core.R as CoreUiR
import java.io.File

/**
 * Class for MimeType
 * @param type detected file type
 * @param extension extension of file
 */
class MimeTypeList private constructor(val type: String, val extension: String) {

    private var resId: Int = -1

    companion object {
        private const val MAX_SIZE_OPENABLE_TEXT_FILE = 20971520

        /**
         * Returns mimetype of file
         * @param file file whose mime type needs to te returned
         * @return mimeType of [File]
         */
        @JvmStatic
        fun getMimeType(file: File): String? {
            val selectedUri = Uri.fromFile(file)
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString())
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
        }

        /**
         * Returns instance of [MimeTypeList]
         * @param name name of file
         */
        @JvmStatic
        fun typeForName(name: String?): MimeTypeList {
            val nonNullName = name ?: ""
            return nonNullName.let {
                val fixedName = it.trim().lowercase()
                val index = fixedName.lastIndexOf(".")
                val extension =
                    if (index != TextFormatterUtils.INVALID_INDEX && index + 1 < fixedName.length) {
                        fixedName.substring(index + 1)
                    } else ""
                val detectedType = getMimeType(
                    extension,
                    MimeTypeMap.getSingleton()
                    ::getMimeTypeFromExtension
                )
                MimeTypeList(detectedType, extension)
            }
        }

        /**
         * List of extension text files
         */
        private val TEXT_EXTENSIONS = listOf(
            //Text
            "txt",
            "ans",
            "ascii",
            "log",
            "wpd",
            "json",
            "md",
            //Web data
            "html",
            "xml",
            "shtml",
            "dhtml",
            "js",
            "css",
            "jar",
            "java",
            "class",
            //Web lang
            "php",
            "php3",
            "php4",
            "php5",
            "phtml",
            "inc",
            "asp",
            "pl",
            "cgi",
            "py",
            "sql",
            "accdb",
            "db",
            "dbf",
            "mdb",
            "pdb",
            "c",
            "cpp",
            "h",
            "cs",
            "sh",
            "vb",
            "swift"
        )

        private val resourcesCache = mutableMapOf<String, Int>()
        private val resources: SparseArray<Array<String>> = SparseArray<Array<String>>().apply {
            put(R.drawable.ic_3d_list, arrayOf("3ds", "3dm", "max", "obj"))
            put(
                R.drawable.ic_aftereffects_list,
                arrayOf("aec", "aep", "aepx", "aes", "aet", "aetx")
            )
            put(
                R.drawable.ic_audio_list,
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
                )
            )
            put(R.drawable.ic_cad_list, arrayOf("dwg", "dxf"))
            put(
                R.drawable.ic_compressed_list,
                arrayOf(
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
                    "pkg",
                )
            )
            put(R.drawable.ic_database_list, arrayOf("accdb", "db", "dbf", "mdb", "pdb", "sql"))
            put(R.drawable.ic_dmg_list, arrayOf("dmg"))
            put(R.drawable.ic_dreamweaver_list, arrayOf("dwt"))
            put(
                R.drawable.ic_excel_list,
                arrayOf("xla", "xlam", "xll", "xlm", "xls", "xlsm", "xlsx", "xlt", "xltm", "xltx")
            )
            put(
                R.drawable.ic_executable_list,
                arrayOf("apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf")
            )
            put(R.drawable.ic_web_lang_list, arrayOf("as", "asc", "ascs"))
            put(R.drawable.ic_flash_list, arrayOf("fla"))
            put(R.drawable.ic_font_list, arrayOf("fnt", "fon", "otf", "ttf"))
            put(R.drawable.ic_gis_list, arrayOf("gpx", "kml", "kmz"))
            put(R.drawable.ic_html_list, arrayOf("dhtml", "htm", "html", "shtml", "xhtml"))
            put(R.drawable.ic_illustrator_list, arrayOf("ai", "aia", "aip", "ait", "art", "irs"))
            put(
                R.drawable.ic_image_list,
                arrayOf("jpg", "jpeg", "tga", "tif", "tiff", "bmp", "gif", "png")
            )
            put(R.drawable.ic_indesign_list, arrayOf("indd"))
            put(R.drawable.ic_java_list, arrayOf("class", "jar", "java"))
            put(R.drawable.ic_midi_list, arrayOf("mid", "midi"))
            put(CoreUiR.drawable.ic_pdf_list, arrayOf("pdf"))
            put(R.drawable.ic_photoshop_list, arrayOf("abr", "csh", "psb", "psd"))
            put(R.drawable.ic_playlist_list, arrayOf("asx", "m3u", "pls"))
            put(R.drawable.ic_podcast_list, arrayOf("pcast"))
            put(
                R.drawable.ic_powerpoint_list,
                arrayOf(
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
                )
            )
            put(R.drawable.ic_premiere_list, arrayOf("plb", "ppj", "prproj", "prtpset"))
            put(
                R.drawable.ic_raw_list,
                arrayOf(
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
                )
            )
            put(R.drawable.ic_real_audio_list, arrayOf("ra", "ram", "rm"))
            put(
                R.drawable.ic_source_list,
                arrayOf("c", "cc", "cgi", "cpp", "cxx", "dll", "h", "hpp", "pl", "py", "sh")
            )
            put(
                R.drawable.ic_spreadsheet_list,
                arrayOf("123", "gsheet", "nb", "ods", "ots", "sxc", "xlr")
            )
            put(R.drawable.ic_subtitles_list, arrayOf("srt"))
            put(R.drawable.ic_swf_list, arrayOf("swf", "flv"))
            put(R.drawable.ic_text_list, arrayOf("ans", "ascii", "log", "rtf", "txt", "wpd"))
            put(R.drawable.ic_torrent_list, arrayOf("torrent"))
            put(R.drawable.ic_vcard_list, arrayOf("vcard", "vcf"))
            put(R.drawable.ic_vector_list, arrayOf("cdr", "eps", "ps", "svg", "svgz"))
            put(
                R.drawable.ic_video_list,
                arrayOf(
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
                    "vob",
                    "movie",
                )
            )
            put(R.drawable.ic_video_vob_list, arrayOf("vob"))
            put(
                R.drawable.ic_web_data_list,
                arrayOf(
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
                )
            )
            put(R.drawable.ic_word_list, arrayOf("doc", "docm", "docx", "dot", "dotx", "wps"))
            put(R.drawable.ic_pages_list, arrayOf("pages"))
            put(R.drawable.ic_experiencedesign_list, arrayOf("Xd"))
            put(R.drawable.ic_keynote_list, arrayOf("key"))
            put(R.drawable.ic_numbers_list, arrayOf("numbers"))
            put(R.drawable.ic_openoffice_list, arrayOf("odp", "odt", "ods"))
            put(R.drawable.ic_sketch_list, arrayOf("sketch"))
            put(R.drawable.ic_url_list, arrayOf("url"))
        }
    }

    /**
     * Return if type is image
     */
    val isImage
        get() = type.startsWith("image/")

    /**
     * Return if type is URL
     */
    val isURL
        get() = type.startsWith("web/url")

    /**
     * Return if type is PDF
     */
    val isPdf
        get() = type.startsWith("application/pdf")

    /**
     * Return if type is zip
     */
    val isZip
        get() = (type.startsWith("application/zip") || type.startsWith("multipart/x-zip"))

    /**
     * Return if type is video
     */
    val isVideo
        get() = type.startsWith("video/")

    /**
     * Check if MimeType is Video
     */
    val isVideoMimeType
        get() =
            type.startsWith("video/") || extension == "vob"

    /**
     * Check if MimeType is svg
     */
    val isSvgMimeType = extension == "svg"

    /**
     * Returns if type is non supported video
     */
    val isVideoNotSupported get() = extension == "mpg" || extension == "wmv"

    /**
     * Return if type is mp4 video
     */
    val isMp4Video get() = type.startsWith("video/") || extension == "mp4"

    /**
     * Return if type is Audio
     */
    val isAudio get() = type.startsWith("audio/") || extension == "opus" || extension == "weba"

    /**
     * Return if type is Audio clip
     */
    val isAudioVoiceClip get() = extension == "m4a"

    /**
     * Return if type is Non supported audio
     */
    val isAudioNotSupported
        get() = extension == "wma" || extension == "aif" || extension == "aiff"
                || extension == "iff" || extension == "oga" || extension == "3ga"

    /**
     * Return icon from [resources]
     */
    val iconResourceId: Int
        get() {
            if (resId == -1) {
                if (resourcesCache.containsKey(extension)) {
                    resId = resourcesCache[extension]!!
                } else {
                    var i = 0
                    val len = resources.size()
                    while (i < len) {
                        val keyResId = resources.keyAt(i)
                        for (valueExtension in resources[keyResId]) {
                            if (extension == valueExtension) {
                                resId = keyResId
                                resourcesCache[extension] = resId
                                break
                            }
                        }
                        if (resId != -1) {
                            break
                        }
                        i++
                    }
                }
                if (resId == -1) {
                    resId = CoreUiR.drawable.ic_generic_list
                }
            }
            return resId
        }

    /**
     * Return if type is GIF
     */
    val isGIF get() = extension == "gif" || extension == "webp"

    /**
     * Checks if a file is openable in Text editor.
     * All the contemplated extension are supported by Web client, so mobile clients should try
     * to support them too.
     * @return True if the file is openable, false otherwise.
     */
    val isValidTextFileType
        get() = type.startsWith(Constants.TYPE_TEXT_PLAIN)
                //File extensions considered as plain text
                || TEXT_EXTENSIONS.contains(extension)
                //Files without extension
                || type.startsWith("application/octet-stream")

    /**
     * Checks if a file is openable in Text editor.
     * It's openable if its size is not bigger than MAX_SIZE_OPENABLE_TEXT_FILE.
     *
     * @return True if the file is openable, false otherwise.
     */
    fun isOpenableTextFile(fileSize: Long) =
        isValidTextFileType && fileSize <= MAX_SIZE_OPENABLE_TEXT_FILE
}