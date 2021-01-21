package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.interfaces.MoveTransferInterface
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.StringResourcesUtils.getTranslatedErrorString
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequest.TYPE_MOVE_TRANSFER

class MoveTransferListener(
    context: Context,
    private val moveTransferInterface: MoveTransferInterface
) : BaseListener(context) {

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type != TYPE_MOVE_TRANSFER) {
            return
        }

        if (e.errorCode != API_OK) {
            logError("Error changing transfer priority: " + getTranslatedErrorString(e))
            moveTransferInterface.movementFailed(request.transferTag)
        } else {
            moveTransferInterface.movementSuccess(request.transferTag)
        }
    }
}