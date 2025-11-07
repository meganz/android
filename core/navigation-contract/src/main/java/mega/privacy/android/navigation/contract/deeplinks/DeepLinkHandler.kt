package mega.privacy.android.navigation.contract.deeplinks

import android.net.Uri
import androidx.navigation3.runtime.NavKey

/**
 * Deep link handler interface to be implemented by feature modules.
 */
interface DeepLinkHandler {
    /**
     * Get the NavKeys from the given Uri if it is a valid deep link for this feature.
     * A list of NavKeys can be added if the Uri points to a detail destination that needs the parent to be in the backStack
     *
     * @param uri The Uri to check
     * @return The NavKeys if the Uri is valid, null otherwise
     */
    suspend fun getNavKeysFromUri(uri: Uri): List<NavKey>?

    /**
     * Priority of this deep link handlers. Lower values will be handled first. Use big value if you want to handle it last if no other handler can handle it.
     *
     */
    val priority get() = 10
}