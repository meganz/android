package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class RenameNodeListener(private val snackbarShower: SnackbarShower, context: Context) :
    BaseListener(context) {
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_MOVE) {
            snackbarShower.showSnackbar(
                context.getString(
                    if (e.errorCode == MegaError.API_OK) R.string.context_correctly_renamed
                    else R.string.context_no_renamed
                )
            )
        }
    }
}
