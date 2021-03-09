package mega.privacy.android.app.listeners

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class CopyNodeListener(
    private val snackbarShower: SnackbarShower,
    private val activityLauncher: ActivityLauncher,
    context: Context
) :
    BaseListener(context) {
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_COPY) {
            when (e.errorCode) {
                MegaError.API_OK -> {
                    snackbarShower.showSnackbar(getString(R.string.context_correctly_copied))
                }
                MegaError.API_EOVERQUOTA -> {
                    val intent = Intent(context, ManagerActivityLollipop::class.java)
                    intent.action = Constants.ACTION_OVERQUOTA_STORAGE
                    activityLauncher.launchActivity(intent)
                }
                MegaError.API_EGOINGOVERQUOTA -> {
                    val intent = Intent(context, ManagerActivityLollipop::class.java)
                    intent.action = Constants.ACTION_PRE_OVERQUOTA_STORAGE
                    activityLauncher.launchActivity(intent)
                }
                else -> {
                    snackbarShower.showSnackbar(getString(R.string.context_no_copied))
                }
            }
        }
    }
}
