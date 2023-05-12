package mega.privacy.android.app.listeners

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber


/**
 * Simple implementation for MegaRequestListenerInterface.
 * For most of scenarios, only need to provide a callback handles with request successfully case and request type.
 *
 * @param requestType Indicate this listener handles which request.
 *                    If the real request type doesn't match the value, then the result will be ignored.
 * @param onSuccess What to do if the request finished successfully.
 * @param onFail What to do if the request finished with error. By default, output log.
 * @param isSuccess How to determine if a request is successful. By default, if the error code is MegaError.ERROR_OK, then consider it's successful.
 */
class SimpleMegaRequestListener(
    private val requestType: Int,
    private val onSuccess: (
        api: MegaApiJava,
        request: MegaRequest,
        e: MegaError,
    ) -> Unit,
    private val onFail: (
        api: MegaApiJava,
        request: MegaRequest,
        e: MegaError,
    ) -> Unit = { _, request, e ->
        Timber.e("[${request.requestString}] failed with error, result: ${e.errorCode} -> ${e.errorString}")
    },
    private val isSuccess: (
        request: MegaRequest,
        e: MegaError,
    ) -> Boolean = { _, e ->
        e.errorCode == MegaError.API_OK
    },
) : MegaRequestListenerInterface {

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("Start [${request.requestString}]")
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {

    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (isSuccess(request, e)) {
            Timber.d("[${request.requestString}] finished successfully.")
            onSuccess(api, request, e)
        } else {
            onFail(api, request, e)
        }
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava,
        request: MegaRequest,
        e: MegaError,
    ) {

    }
}