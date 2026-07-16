package mega.privacy.android.data.test.gateway

import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

/**
 * Outcome delivered to the [nz.mega.sdk.MegaRequestListenerInterface] of a listener-based
 * [mega.privacy.android.data.gateway.api.MegaApiGateway] method.
 *
 * @property request the request passed to the listener callbacks; when null the fake supplies a
 * [mega.privacy.android.data.test.stub.StubMegaRequest] with the method's matching
 * [MegaRequest].TYPE_* constant.
 * @property error the error passed to onRequestFinish.
 */
data class MegaRequestOutcome(
    val request: MegaRequest?,
    val error: MegaError,
)
