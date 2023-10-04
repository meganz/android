package mega.privacy.android.feature.sync.data.mock

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

// Mock class to avoid dependency on SDK SRW branch
class StalledIssuesReceiver(
    private val onStallListLoaded: (MegaSyncStallList) -> Unit,
) : MegaRequestListenerInterface {

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {

    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {

    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {

    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {

    }
}