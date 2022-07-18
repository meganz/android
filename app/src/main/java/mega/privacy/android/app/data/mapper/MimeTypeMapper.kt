package mega.privacy.android.app.data.mapper

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
    extension.lowercase().let { defaultMapper(it) ?: getCustomTypes(it) }

private fun getCustomTypes(extension: String) = when (extension) {
    "url" -> "web/url"
    else -> "application/octet-stream"
}
