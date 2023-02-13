package mega.privacy.android.data.extensions

import android.util.Base64

/**
 * Encode String to Base64
 */
fun String.encodeBase64(): String =
    Base64.encodeToString(this.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)