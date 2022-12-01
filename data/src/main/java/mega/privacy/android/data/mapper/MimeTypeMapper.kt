package mega.privacy.android.data.mapper

/**
 * Map extension to mime type
 */
typealias MimeTypeMapper = (@JvmSuppressWildcards String) -> @JvmSuppressWildcards String

/**
 * Map extension to nullable mime type
 */
typealias DefaultMimeTypeMapper = (String) -> String?


/**
 * Get mime type for extension
 *
 * @param extension
 * @param defaultMapper
 * @receiver
 * @return
 */
internal fun getMimeType(extension: String, defaultMapper: (String) -> String?) =
    extension.lowercase().let {
        if (it == "dcr") {
            "image/dcr"
        } else {
            defaultMapper(it) ?: getCustomTypes(it)
        }
    }


private fun getCustomTypes(extension: String) = when (extension) {
    "mkv" -> "video/x-matroska"
    "heic" -> "image/heic"
    "url" -> "web/url"
    "webp" -> "image/webp"
    "3fr" -> "image/3fr"
    "iiq" -> "image/iiq"
    "k25" -> "image/k25"
    "kdc" -> "image/kdc"
    "mef" -> "image/mef"
    "mos" -> "image/mos"
    "mrw" -> "image/mrw"
    "raw" -> "image/raw"
    "rwl" -> "image/rwl"
    "sr2" -> "image/sr2"
    "srf" -> "image/srf"
    "x3f" -> "image/x3f"
    "cr3" -> "image/cr3"
    "ciff" -> "image/ciff"
    else -> "application/octet-stream"
}
