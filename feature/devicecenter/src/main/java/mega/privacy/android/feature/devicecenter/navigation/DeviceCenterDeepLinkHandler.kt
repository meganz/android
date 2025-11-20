package mega.privacy.android.feature.devicecenter.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.destination.DeviceCenterNavKey
import javax.inject.Inject

class DeviceCenterDeepLinkHandler @Inject constructor() : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? =
        if (regexPatternType == RegexPatternType.OPEN_DEVICE_CENTER_LINK) {
            listOf(DeviceCenterNavKey)
        } else null
}