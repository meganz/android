package mega.privacy.android.app.listeners

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

class OptionalMegaRequestListenerInterface(
    private val onRequestStart: ((MegaRequest) -> Unit)? = null,
    private val onRequestUpdate: ((MegaRequest) -> Unit)? = null,
    private val onRequestTemporaryError: ((MegaRequest, MegaError) -> Unit)? = null,
    private val onRequestFinish: ((MegaRequest, MegaError) -> Unit)? = null
) : MegaRequestListenerInterface {

    override fun onRequestStart(
        api: MegaApiJava,
        request: MegaRequest
    ) {
        onRequestStart?.invoke(request)
    }

    override fun onRequestUpdate(
        api: MegaApiJava,
        request: MegaRequest
    ) {
        onRequestUpdate?.invoke(request)
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError
    ) {
        onRequestTemporaryError?.invoke(request, error)
    }

    override fun onRequestFinish(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError
    ) {
        onRequestFinish?.invoke(request, error)
    }
}
