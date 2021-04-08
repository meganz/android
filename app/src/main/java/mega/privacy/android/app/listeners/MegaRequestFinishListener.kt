package mega.privacy.android.app.listeners

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class MegaRequestFinishListener(
    private val onSuccess: (request: MegaRequest) -> Unit,
    private val onError: ((Int) -> Unit)? = null
) : BaseListener(null) {
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (e.errorCode == MegaError.API_OK) {
            onSuccess(request)
        } else {
            onError?.invoke(e.errorCode)
        }
    }
}
