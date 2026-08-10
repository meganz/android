package mega.privacy.android.app.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import mega.privacy.android.app.activities.OverDiskQuotaPaywallActivity
import mega.privacy.android.navigation.AppNavigator

internal interface AppNavigatorImpl : AppNavigator {

    override fun openNodeInBackups(
        activity: Activity,
        backupsHandle: Long,
        @StringRes errorMessage: Int?,
    ) {
    }

    override fun openNodeInCloudDrive(
        activity: Activity,
        nodeHandle: Long,
        @StringRes errorMessage: Int?,
        isFromSyncFolders: Boolean,
    ) {
    }

    override fun openOverDiskQuotaPaywallWarning(context: Context) {
        val intent = Intent(context, OverDiskQuotaPaywallActivity::class.java)
        context.startActivity(intent)
    }
}
