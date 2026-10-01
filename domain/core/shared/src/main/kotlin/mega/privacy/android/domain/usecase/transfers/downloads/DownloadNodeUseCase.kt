package mega.privacy.android.domain.usecase.transfers.downloads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

class DownloadNodeUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val transferRepository: TransferRepository,
) {
    operator fun invoke(
        node: TypedNode,
        destinationPath: String,
        appData: List<TransferAppData>?,
        isHighPriority: Boolean,
    ): Flow<TransferEvent> = flow {
        fileSystemRepository.createDirectory(destinationPath)
        val finalAppData = if (node is ChatFile) {
            (appData ?: emptyList()) + TransferAppData.ChatDownload(
                node.chatId,
                node.messageId,
                node.messageIndex
            )
        } else appData
        emitAll(
            transferRepository.startDownload(
                node = node,
                localPath = destinationPath,
                appData = finalAppData,
                shouldStartFirst = isHighPriority,
            )
        )
    }
}