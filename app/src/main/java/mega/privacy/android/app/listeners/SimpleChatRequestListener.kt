package mega.privacy.android.app.listeners

import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import timber.log.Timber

/**
 * Simple implementation for MegaChatRequestListenerInterface.
 * For most of scenarios, only need to provide a callback handles with request successfully case and request type.
 *
 * @param requestType Indicate this listener handles which request.
 *                    If the real request type doesn't match the value, then the result will be ignored.
 * @param onSuccess What to do if the request finished successfully.
 * @param onFail What to do if the request finished with error. By default, output log.
 * @param isSuccess How to determine if a request is successful. By default, if the error code is {MegaChatError.ERROR_OK}, then consider it's successful.
 */
class SimpleChatRequestListener(
    private val requestType: Int,
    private val onSuccess: (
        api: MegaChatApiJava,
        request: MegaChatRequest,
        e: MegaChatError,
    ) -> Unit,
    private val onFail: (
        api: MegaChatApiJava,
        request: MegaChatRequest,
        e: MegaChatError,
    ) -> Unit = { _, request, e ->
        Timber.e("[${request.requestString}] failed with error, result: ${e.errorCode} -> ${e.errorString}")
    },
    private val isSuccess: (
        request: MegaChatRequest,
        e: MegaChatError,
    ) -> Boolean = { _, e ->
        e.errorCode == MegaChatError.ERROR_OK
    },
) : MegaChatRequestListenerInterface {

    override fun onRequestStart(api: MegaChatApiJava, request: MegaChatRequest) {
        Timber.d("Start [${request.requestString}]")
    }

    override fun onRequestUpdate(api: MegaChatApiJava?, request: MegaChatRequest) {

    }

    override fun onRequestFinish(
        api: MegaChatApiJava,
        request: MegaChatRequest,
        e: MegaChatError,
    ) {
        if (requestType != request.type) return

        if (isSuccess(request, e)) {
            Timber.d("[${request.requestString}] finished successfully.")
            onSuccess(api, request, e)
        } else {
            onFail(api, request, e)
        }
    }

    override fun onRequestTemporaryError(
        api: MegaChatApiJava?,
        request: MegaChatRequest?,
        e: MegaChatError?,
    ) {

    }
}