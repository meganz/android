package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.domain.entity.transfer.TransferData
import nz.mega.sdk.MegaTransferData
import javax.inject.Inject

/**
 * Mapper for converting [MegaTransferData] into [TransferData].
 */
class TransferDataMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param transferData [MegaTransferData]
     * @return [TransferData]
     */
    operator fun invoke(transferData: MegaTransferData) = with(transferData) {
        TransferData(
            numDownloads = numDownloads,
            numUploads = numUploads,
            downloadTags = mapTags(numDownloads, ::getDownloadTag),
            uploadTags = mapTags(numUploads, ::getUploadTag),
        )
    }

    private fun mapTags(transferCount: Int, getTag: (Int) -> Int): List<Int> =
        (0 until transferCount).map { getTag(it) }
}