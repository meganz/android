package mega.privacy.android.data.test.gateway

import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer

/**
 * Outcome delivered to the [nz.mega.sdk.MegaTransferListenerInterface] of a transfer-listener
 * based [mega.privacy.android.data.gateway.api.MegaApiGateway] method (uploads, downloads,
 * full-image fetches).
 *
 * @property transfer the transfer passed to the listener callbacks; when null the fake supplies an
 * empty [mega.privacy.android.data.test.stub.StubMegaTransfer].
 * @property error the error passed to onTransferFinish.
 */
data class MegaTransferOutcome(
    val transfer: MegaTransfer?,
    val error: MegaError,
)
