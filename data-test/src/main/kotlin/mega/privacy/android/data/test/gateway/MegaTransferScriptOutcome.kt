package mega.privacy.android.data.test.gateway

import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer

/**
 * Scripted outcome for a transfer-listener method: instead of completing immediately, the fake
 * delivers onTransferStart with the first of [steps], then onTransferUpdate for each remaining
 * step every [stepDelayMs], and finally onTransferFinish with [finalTransfer] and [error] — the
 * way the real SDK reports progress while a large file uploads.
 *
 * Configure via [FakeMegaApiGateway.stubTransferScript].
 */
class MegaTransferScriptOutcome(
    val steps: List<MegaTransfer>,
    val finalTransfer: MegaTransfer,
    val error: MegaError,
    val stepDelayMs: Long,
) {
    init {
        require(steps.isNotEmpty()) { "A transfer script needs at least one step" }
    }
}
