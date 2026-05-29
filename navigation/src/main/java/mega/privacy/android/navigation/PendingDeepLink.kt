package mega.privacy.android.navigation

import android.app.Activity
import androidx.core.net.toUri

/**
 * Intent action used to signal that a deep link is pending after login or account creation.
 */
const val ACTION_PENDING_DEEP_LINK: String = "PENDING_DEEP_LINK"

/**
 * Set the current activity's intent to carry a pending deep link that will be
 * re-emitted after login or account creation completes.
 *
 * @param uriString the deep link URI to reopen after authentication.
 * @param mimeType optional mime type to preserve for downstream classification.
 */
fun Activity?.setPendingDeepLink(uriString: String?, mimeType: String? = null) {
    this?.intent?.apply {
        action = ACTION_PENDING_DEEP_LINK
        setDataAndType(uriString?.toUri(), mimeType)
    }
}
