package mega.privacy.android.feature.devicecenter.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.DeviceCenterNavKey
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

class DeviceCenterDeepLinkHandler @Inject constructor(
    private val snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? =
        when (regexPatternType) {
            RegexPatternType.OPEN_DEVICE_CENTER_LINK -> if (isLoggedIn) {
                listOf(DeviceCenterNavKey)
            } else {
                snackbarEventQueue.queueMessage(sharedR.string.general_alert_not_logged_in)
                emptyList()
            }

            else -> null
        }
}