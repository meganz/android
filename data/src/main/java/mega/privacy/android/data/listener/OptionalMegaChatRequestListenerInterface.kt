package mega.privacy.android.data.listener

import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface

/**
 * MegaChatRequestListenerInterface with optional callbacks.
 */
class OptionalMegaChatRequestListenerInterface(
    private val onRequestStart: ((MegaChatRequest) -> Unit)? = null,
    private val onRequestUpdate: ((MegaChatRequest) -> Unit)? = null,
    private val onRequestTemporaryError: ((MegaChatRequest, MegaChatError) -> Unit)? = null,
    private val onRequestFinish: ((MegaChatRequest, MegaChatError) -> Unit)? = null
) : MegaChatRequestListenerInterface {

    override fun onRequestStart(
        api: MegaChatApiJava,
        request: MegaChatRequest
    ) {
        onRequestStart?.invoke(request)
    }

    override fun onRequestUpdate(
        api: MegaChatApiJava,
        request: MegaChatRequest
    ) {
        onRequestUpdate?.invoke(request)
    }

    override fun onRequestTemporaryError(
        api: MegaChatApiJava,
        request: MegaChatRequest,
        error: MegaChatError
    ) {
        onRequestTemporaryError?.invoke(request, error)
    }

    override fun onRequestFinish(
        api: MegaChatApiJava,
        request: MegaChatRequest,
        error: MegaChatError
    ) {
        onRequestFinish?.invoke(request, error)
    }
}
