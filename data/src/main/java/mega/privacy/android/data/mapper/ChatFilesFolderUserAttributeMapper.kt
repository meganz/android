package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaStringMap

/**
 * Map [MegaStringMap] to [String]
 */
typealias ChatFilesFolderUserAttributeMapper = (@JvmSuppressWildcards MegaStringMap?) -> String?

/**
 * Map [MegaStringMap] to [String]
 */
internal fun toChatFilesFolderUserAttribute(map: MegaStringMap?): String? {
    return map?.takeIf { it.size() > 0 && map["h"].isNullOrEmpty().not() }?.let {
        return@let map["h"]
    }
}
