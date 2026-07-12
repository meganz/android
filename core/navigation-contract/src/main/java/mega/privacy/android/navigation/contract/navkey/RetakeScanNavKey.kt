package mega.privacy.android.navigation.contract.navkey

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation key for re-capturing an existing scanned page. Opens the scan camera
 * in retake mode; the next capture replaces the page with [pageId] in place.
 *
 * @property pageId Id of the page being retaken.
 */
@Serializable
data class RetakeScanNavKey(val pageId: String) : NavKey
