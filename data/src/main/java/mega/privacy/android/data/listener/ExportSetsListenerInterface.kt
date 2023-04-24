package mega.privacy.android.data.listener

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

internal class ExportSetsListenerInterface(
    private val totalSets: Int,
    private val onCompletion: (List<Pair<Long, String>>) -> Unit,
) : MegaRequestListenerInterface {
    private val setLinks = mutableListOf<Pair<Long, String>>()

    private var numSets = 0

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, error: MegaError) {
        numSets++

        if (error.errorCode == MegaError.API_OK) {
            val set = request.megaSet
            val link = request.link
            if (set != null && link != null) {
                setLinks.add(set.id() to link)
            }
        }

        if (numSets == totalSets) {
            onCompletion(setLinks)
        }
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError,
    ) {
    }
}

internal class DisableExportSetsListenerInterface(
    private val totalSets: Int,
    private val onCompletion: (success: Int, failure: Int) -> Unit,
) : MegaRequestListenerInterface {
    private var numSets = 0

    private var success = 0

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, error: MegaError) {
        numSets++

        if (error.errorCode == MegaError.API_OK) success++
        if (numSets == totalSets) onCompletion(success, numSets - success)
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError,
    ) {
    }
}
