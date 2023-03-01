package mega.privacy.android.data.extensions

import android.util.Base64
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaStringMap
import timber.log.Timber

/**
 * Decode each alias within MegaStringMap into a Map<Long, String>
 */
fun MegaStringMap.getDecodedAliases(): Map<Long, String> {
    val aliases = mutableMapOf<Long, String>()

    for (i in 0 until keys.size()) {
        val base64Handle = keys[i]
        val handle = MegaApiJava.base64ToUserHandle(base64Handle)
        try {
            aliases[handle] = get(base64Handle).decodeBase64()
        } catch (error: IllegalArgumentException) {
            Timber.w(error)
        }
    }

    return aliases
}

/**
 * Decode the Base64-encoded data into a new formatted String
 */
fun String.decodeBase64(): String =
    try {
        Base64.decode(this.trim(), Base64.DEFAULT).toString(Charsets.UTF_8)
    } catch (ignore: IllegalArgumentException) {
        Base64.decode(this.trim(), Base64.URL_SAFE).toString(Charsets.UTF_8)
    }

/**
 * Decode value in the [MegaStringMap] into a corresponding [String]
 *
 * @param key The [String] key
 *
 * @return The corresponding [String] value, or null if it does not exist
 */
fun MegaStringMap?.getValueFor(key: String) =
    this?.takeIf { it.size() > 0 && this[key].isNullOrEmpty().not() }?.let {
        return@let this[key]
    }