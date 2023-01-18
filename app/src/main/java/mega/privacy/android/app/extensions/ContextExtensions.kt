package mega.privacy.android.app.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.NonNull
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.Util
import timber.log.Timber

/**
 * Navigate to the App's Settings page to manually grant the requested permissions
 */
@NonNull
fun Context.navigateToAppSettings() {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    try {
        startActivity(intent)
    } catch (e: Exception) {
        if (this is ManagerActivity) {
            // in case few devices cannot handle 'ACTION_APPLICATION_DETAILS_SETTINGS' action.
            Util.showSnackbar(
                this,
                getString(R.string.on_permanently_denied)
            )
        } else {
            Timber.e(e, "Exception opening device settings")
        }
    }
}