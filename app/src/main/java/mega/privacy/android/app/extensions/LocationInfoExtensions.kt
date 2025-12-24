package mega.privacy.android.app.extensions

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_FOLDER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FRAGMENT_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_LOCATION_FILE_INFO
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PATH_NAVIGATION
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.navigation.MegaNavigator

@Deprecated("This function will be removed after SingleActivity flag goes live. Note that any calls to it while the flag is enabled will result in an exception")
fun LocationInfo.handleLocationClick(
    activity: Activity,
    adapterType: Int,
    megaNavigator: MegaNavigator,
) {
    megaNavigator.openManagerActivity(
        context = activity,
        action = ACTION_OPEN_FOLDER,
        bundle = Bundle().apply {
            putBoolean(INTENT_EXTRA_KEY_LOCATION_FILE_INFO, true)

            if (adapterType == OFFLINE_ADAPTER) {
                putBoolean(INTENT_EXTRA_KEY_OFFLINE_ADAPTER, true)
                putString(INTENT_EXTRA_KEY_PATH_NAVIGATION, offlineParentPath)
            } else {
                putLong(INTENT_EXTRA_KEY_FRAGMENT_HANDLE, fragmentHandle)
                putLong(INTENT_EXTRA_KEY_PARENT_HANDLE, parentHandle)
            }
        },
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP,
    )

    activity.finish()
}