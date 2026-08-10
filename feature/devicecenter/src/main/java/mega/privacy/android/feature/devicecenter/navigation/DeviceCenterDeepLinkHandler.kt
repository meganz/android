package mega.privacy.android.feature.devicecenter.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.DeviceCenterNavKey
import javax.inject.Inject

class DeviceCenterDeepLinkHandler @Inject constructor(
    snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler(snackbarEventQueue) {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? =
        when (regexPatternType) {
            RegexPatternType.OPEN_DEVICE_CENTER_LINK -> listOf(DeviceCenterNavKey)

            else -> null
        }
}