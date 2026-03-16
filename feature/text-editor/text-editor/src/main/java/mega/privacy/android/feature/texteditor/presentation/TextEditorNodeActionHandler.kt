package mega.privacy.android.feature.texteditor.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorNodeActionRequest
import mega.privacy.android.navigation.contract.TransferHandler
import timber.log.Timber
import javax.inject.Inject

/**
 * Handles node action requests from the Compose text editor bottom bar.
 * Download: resolves node and triggers transfer; GetLink/Share can be wired in a follow-up.
 */
class TextEditorNodeActionHandler @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) {

    fun handle(
        scope: CoroutineScope,
        request: TextEditorNodeActionRequest,
        transferHandler: TransferHandler,
    ) {
        when (request) {
            is TextEditorNodeActionRequest.Download -> scope.launch {
                val node = getNodeByIdUseCase(NodeId(request.nodeHandle))
                if (node != null) {
                    transferHandler.setTransferEvent(
                        TransferTriggerEvent.StartDownloadNode(
                            nodes = listOf(node),
                            withStartMessage = true,
                        ),
                    )
                } else {
                    Timber.w("Text editor: node %d not found for download", request.nodeHandle)
                }
            }
            is TextEditorNodeActionRequest.GetLink,
            is TextEditorNodeActionRequest.Share -> {
                // TODO: Wire get-link and share flows (e.g. open Get Link screen, system share)
            }
        }
    }
}
