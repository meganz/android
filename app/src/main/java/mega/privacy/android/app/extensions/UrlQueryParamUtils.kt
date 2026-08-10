package mega.privacy.android.app.extensions

import android.net.Uri

private val MEGA_DOMAINS_FOR_NOPLANS = setOf(
    "mega.io",
    "help.mega.io",
    "mega.co.nz",
    "www.mega.io",
    "www.help.mega.io",
    "www.mega.co.nz",
)

/**
 * Appends noplans=1 query parameter to MEGA domain URIs to suppress checkout redirects.
 * Used when opening external links (help centre, ToS, etc.) from the app.
 *
 * @param uri The URI to potentially modify
 * @return The URI with noplans=1 appended for mega.io/help.mega.io/mega.co.nz, unchanged otherwise
 */
internal fun appendNoPlansParam(uri: Uri): Uri {
    val host = uri.host ?: return uri
    if (host.lowercase() !in MEGA_DOMAINS_FOR_NOPLANS) return uri
    return uri.buildUpon().appendQueryParameter("noplans", "1").build()
}
