package mega.privacy.android.domain.usecase.transfers.previews

import mega.privacy.android.domain.entity.transfer.isPreviewDownload
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersFromSdkUseCase
import javax.inject.Inject

/**
 * Cancels the preview downloads currently in progress. The caller decides when that is right: it
 * is meant for bandwidth over quota, where a preview download cannot advance.
 *
 * Unfinished transfers are cached and resumed in the next session, but the screen waiting for the
 * preview died with the process, so a stalled one would only keep opening the loading preview
 * screen on every app start.
 */
class CancelOverQuotaPreviewDownloadsUseCase @Inject constructor(
    private val getInProgressTransfersFromSdkUseCase: GetInProgressTransfersFromSdkUseCase,
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase,
) {

    /**
     * Invoke
     *
     * @return the tags of the cancelled preview downloads, empty if there was nothing to cancel.
     */
    suspend operator fun invoke(): List<Int> =
        getInProgressTransfersFromSdkUseCase()
            .filter { it.isPreviewDownload() }
            .map { it.tag }
            // A tag the SDK already dropped throws, which must not skip the remaining cancellations
            .filter { tag -> runCatching { cancelTransferByTagUseCase(tag) }.isSuccess }
}
