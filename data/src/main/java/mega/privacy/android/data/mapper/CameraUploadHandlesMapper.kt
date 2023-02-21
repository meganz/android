package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaStringMap

/**
 * Map [MegaStringMap] to [Pair]
 */
typealias CameraUploadHandlesMapper = (@JvmSuppressWildcards MegaStringMap?) -> Pair<String?, String?>

/**
 * Map [MegaStringMap] to [Pair]
 * [MegaStringMap] with keys "h" and "sh" for camera upload handle/secondary handle
 */
internal fun toCameraUploadHandles(map: MegaStringMap?) =
    Pair(map.getValueFor("h"), map.getValueFor("sh"))

private fun MegaStringMap?.getValueFor(key: String) =
    this?.takeIf { it.size() > 0 && this[key].isNullOrEmpty().not() }?.let {
        return@let this[key]
    }
