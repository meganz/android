package mega.privacy.android.app.presentation.transfers.model.image

import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import javax.inject.Inject

/**
 * ViewModel for Transfer items.
 *
 */
@HiltViewModel
class InProgressTransferImageViewModel @Inject constructor(
    getThumbnailUseCase: GetThumbnailUseCase,
    fileTypeIconMapper: FileTypeIconMapper,
) : AbstractTransferImageViewModel(
    getThumbnailUseCase = getThumbnailUseCase,
    fileTypeIconMapper = fileTypeIconMapper,
) {

    /**
     * Add a new in progress transfer to the UI state.
     */
    override fun <T> addTransfer(transfer: T) {
        with(transfer as InProgressTransfer) {
            if (this@with is InProgressTransfer.Download) {
                addNodeTransfer(tag, fileName, nodeId)
            } else {
                addFileTransfer(tag, fileName, (this@with as InProgressTransfer.Upload).localPath)
            }
        }
    }
}