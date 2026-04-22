package mega.privacy.android.app.extensions

import android.app.Activity
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.OfflineNavKey

@Deprecated("This function will be removed after SingleActivity flag goes live. Note that any calls to it while the flag is enabled will result in an exception")
@Suppress("UNUSED_PARAMETER")
fun LocationInfo.handleLocationClick(
    activity: Activity,
    adapterType: Int,
    megaNavigator: MegaNavigator,
) {
    val intent = if (adapterType == OFFLINE_ADAPTER) {
        MegaActivity.getIntentWithExtraDestinations(
            activity,
            listOf(OfflineNavKey(path = offlineParentPath)),
        )
    } else {
        MegaActivity.getIntentWithExtraDestinations(
            activity,
            listOf(CloudDriveNavKey(nodeHandle = parentHandle)),
        )
    }
    activity.startActivity(intent)

    activity.finish()
}