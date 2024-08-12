package mega.privacy.android.app.presentation.transfers.model.image

import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.data.mapper.transfer.TransferStateMapper
import mega.privacy.android.data.mapper.transfer.TransferTypeMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import javax.inject.Inject

/**
 * ViewModel for Transfer items.
 *
 */
@HiltViewModel
class CompletedTransferImageViewModel @Inject constructor(
    getThumbnailUseCase: GetThumbnailUseCase,
    fileTypeIconMapper: FileTypeIconMapper,
    private val transferStateMapper: TransferStateMapper,
    private val transferTypeMapper: TransferTypeMapper,
) : AbstractTransferImageViewModel(
    getThumbnailUseCase = getThumbnailUseCase,
    fileTypeIconMapper = fileTypeIconMapper,
) {

    /**
     * Adds a new completed transfer to the UI state.
     */
    override fun <T> addTransfer(transfer: T) {
        with(transfer as CompletedTransfer) {
            id?.let {
                if (transferStateMapper(state) == TransferState.STATE_COMPLETED
                    || transferTypeMapper(type, emptyList()) == TransferType.DOWNLOAD
                ) {
                    addNodeTransfer(it, fileName, NodeId(handle))
                } else {
                    addFileTransfer(it, fileName, originalPath)
                }
            }
        }

    }
}