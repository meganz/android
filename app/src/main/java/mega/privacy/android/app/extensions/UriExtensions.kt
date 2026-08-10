package mega.privacy.android.app.extensions

import android.net.Uri

/**
 * Returns true if this URI uses the http or https scheme.
 */
internal fun Uri.isHttpScheme(): Boolean = scheme == "http" || scheme == "https"
