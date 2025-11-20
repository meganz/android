package mega.privacy.android.app.presentation.settings

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.SettingsCameraUploadsNavKey
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Deep Link Handler for Settings
 */
class SettingsDeepLinkHandler @Inject constructor(
    private val snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? = when (regexPatternType) {
        RegexPatternType.ENABLE_CAMERA_UPLOADS_LINK -> if (isLoggedIn) {
            listOf(SettingsCameraUploadsNavKey)
        } else {
            snackbarEventQueue.queueMessage(sharedR.string.general_alert_not_logged_in)
            emptyList()
        }

        else -> null
    }
}