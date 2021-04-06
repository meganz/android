package mega.privacy.android.app.listeners

import android.content.Intent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.StringResourcesUtils.getTranslatedErrorString
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class RemoveListener(
    private val snackbarShower: SnackbarShower? = null,
    private val isIncomingShare: Boolean = false,
    private val onFinish: ((Boolean) -> Unit)? = null
) : BaseListener(MegaApplication.getInstance()) {

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type != MegaRequest.TYPE_REMOVE) {
            return
        }

        if (isIncomingShare) {
            if (e.errorCode == MegaError.API_OK) {
                val intent = Intent(BroadcastConstants.BROADCAST_ACTION_SHOW_SNACKBAR)
                intent.putExtra(BroadcastConstants.SNACKBAR_TEXT, getString(R.string.share_left))
                context.sendBroadcast(intent)
            } else {
                snackbarShower?.showSnackbar(getTranslatedErrorString(e))
            }

            return
        }

        onFinish?.invoke(e.errorCode == MegaError.API_OK)
    }
}
