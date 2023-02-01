package mega.privacy.android.data.listener

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

internal class CreateSetElementListenerInterface(
    private val target: Int,
    private val onCompletion: (success: Int, failure: Int) -> Unit,
) : MegaRequestListenerInterface {
    private var total = 0

    private var success = 0

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, error: MegaError) {
        total++
        if (error.errorCode == MegaError.API_OK) success++

        if (total == target) {
            onCompletion(success, total - success)
        }
    }

    override fun onRequestTemporaryError(api: MegaApiJava, p1: MegaRequest, p2: MegaError) {}
}
