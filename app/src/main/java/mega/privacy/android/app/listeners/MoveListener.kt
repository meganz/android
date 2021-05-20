package mega.privacy.android.app.listeners

import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_EOVERQUOTA
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaRequest

class MoveListener(
    private val snackbarShower: SnackbarShower? = null,
    private val onFinish: ((Boolean, Boolean) -> Unit)? = null
) : BaseListener(MegaApplication.getInstance()) {

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_MOVE) {
            snackbarShower?.showSnackbar(
                getString(
                    if (e.errorCode == API_OK) R.string.context_correctly_moved
                    else R.string.context_no_moved
                )
            )

            onFinish?.invoke(
                e.errorCode == API_OK,
                e.errorCode == API_EOVERQUOTA && api.isForeignNode(request.parentHandle)
            )
        }
    }
}
