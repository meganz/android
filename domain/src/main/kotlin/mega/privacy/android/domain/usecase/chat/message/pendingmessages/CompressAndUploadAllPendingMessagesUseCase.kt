package mega.privacy.android.domain.usecase.chat.message.pendingmessages

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.transfer.ChatCompressionFinished
import mega.privacy.android.domain.entity.transfer.ChatCompressionProgress
import mega.privacy.android.domain.entity.transfer.ChatCompressionState
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.usecase.chat.ChatUploadCompressionState
import mega.privacy.android.domain.usecase.chat.message.MonitorPendingMessagesByStateUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.CompressFileForChatUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.HandleChatUploadTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.UploadFilesUseCase
import java.io.File
import javax.inject.Inject

/**
 * Compress all pending messages in compressing state and then start uploading them, returning a flow to track the compression progress.
 * If there are new pending messages that needs compression added while this is running they will be compressed as well.
 * When there are no more pending messages that needs compression a ChatCompressionFinished event is emitted and the flow ends.
 */
class CompressAndUploadAllPendingMessagesUseCase @Inject constructor(
    private val compressFileForChatUseCase: CompressFileForChatUseCase,
    private val uploadFilesUseCase: UploadFilesUseCase,
    private val handleChatUploadTransferEventUseCase: HandleChatUploadTransferEventUseCase,
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase,
    private val monitorPendingMessagesByStateUseCase: MonitorPendingMessagesByStateUseCase,
) {

    /**
     * Invoke
     * @return a flow to track the compression progress
     */
    operator fun invoke(): Flow<ChatCompressionState> {
        val pathsToSizeAndProgress = mutableMapOf<String, SizeAndProgress>()
        return channelFlow {
            monitorPendingMessagesByStateUseCase(PendingMessageState.COMPRESSING)
                .conflate()
                .collect { pendingMessageList ->
                    val semaphore = Semaphore(5) // to limit the parallel compressions
                    pendingMessageList
                        .groupBy { it.filePath }
                        .map { (path, pendingMessages) ->
                            //compress all received pending message's files in parallel, new pending messages will wait for this batch to be completed.
                            launch {
                                if (path !in pathsToSizeAndProgress) {
                                    semaphore.withPermit {
                                        //if this was already compressed don't compress it again.
                                        val original = File(path)
                                        pathsToSizeAndProgress[path] = SizeAndProgress(
                                            original.length().coerceAtLeast(1L),
                                            Progress(0f)
                                        )
                                        val totalSize =
                                            pathsToSizeAndProgress.values.sumOf { it.size }
                                        val compressed = compressFileForChatUseCase(original)
                                            .onEach { chatUploadCompressionState ->
                                                val newProgress =
                                                    when (chatUploadCompressionState) {
                                                        is ChatUploadCompressionState.Compressing -> {
                                                            chatUploadCompressionState.progress
                                                        }

                                                        is ChatUploadCompressionState.Compressed, is ChatUploadCompressionState.NotCompressed -> {
                                                            Progress(1f)
                                                        }
                                                    }
                                                pathsToSizeAndProgress[path]?.copy(
                                                    progress = newProgress
                                                )?.let { progressUpdated ->
                                                    pathsToSizeAndProgress[path] = progressUpdated
                                                }
                                                send(
                                                    ChatCompressionProgress(
                                                        alreadyCompressed = pathsToSizeAndProgress.count { it.value.finished },
                                                        totalToCompress = pathsToSizeAndProgress.size,
                                                        progress = Progress(
                                                            current = pathsToSizeAndProgress.values.sumOf { it.progressBytes },
                                                            total = totalSize
                                                        )
                                                    )
                                                )
                                            }
                                            .mapNotNull {
                                                (it as? ChatUploadCompressionState.Compressed)?.file
                                            }.firstOrNull()
                                        // Once the file is compressed, let's start the upload
                                        val filesAndNames =
                                            mapOf(
                                                (compressed
                                                    ?: original) to pendingMessages.firstOrNull()?.name
                                            )
                                        uploadFilesUseCase(
                                            filesAndNames,
                                            getMyChatsFilesFolderIdUseCase(),
                                            pendingMessages.map { TransferAppData.ChatUpload(it.id) },
                                            false
                                        ).filterIsInstance<MultiTransferEvent.SingleTransferEvent>()
                                            .firstOrNull() // Wait until the transfer has started to be sure we handle the event (to save the transfer tag for instance)
                                            ?.let { firstEvent ->
                                                handleChatUploadTransferEventUseCase(
                                                    firstEvent,
                                                    *pendingMessages.map { it.id }.toLongArray()
                                                )
                                            }
                                    }
                                }
                            }
                        }.joinAll()
                    if (pendingMessageList.isEmpty()) {
                        yield()
                        send(ChatCompressionFinished)
                    }
                }
        }
    }

    private data class SizeAndProgress(val size: Long, val progress: Progress) {
        val finished = progress.floatValue == 1f
        val progressBytes = progress.floatValue.toDouble() * size
    }
}