package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaStringMap

/**
 * Map [MegaStringMap] to [String]
 */
typealias ChatFilesFolderUserAttributeMapper = (@JvmSuppressWildcards MegaStringMap?) -> String?

/**
 * Map [MegaStringMap] to [String]
 * [MegaStringMap] where one of the entries will contain a key "h" and its value, the handle in base64.
 */
internal fun toChatFilesFolderUserAttribute(map: MegaStringMap?): String? {
    return map?.takeIf { it.size() > 0 && map["h"].isNullOrEmpty().not() }?.let {
        return@let map["h"]
    }
}
