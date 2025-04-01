package mega.privacy.android.domain.usecase.chat.message.pendingmessages

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
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
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateAndPathRequest
import mega.privacy.android.domain.entity.transfer.ChatCompressionFinished
import mega.privacy.android.domain.entity.transfer.ChatCompressionProgress
import mega.privacy.android.domain.entity.transfer.ChatCompressionState
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.ChatUploadCompressionState
import mega.privacy.android.domain.usecase.chat.message.MonitorPendingMessagesByStateUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import mega.privacy.android.domain.usecase.file.FileResult
import mega.privacy.android.domain.usecase.file.GetFileSizeFromUriPathUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.CompressFileForChatUseCase
import javax.inject.Inject

/**
 * Compress all pending messages in compressing state and then set it to ready to upload, returning a flow to track the compression progress.
 * If there are new pending messages that needs compression added while this is running they will be compressed as well.
 * When there are no more pending messages that needs compression a ChatCompressionFinished event is emitted.
 */
class CompressPendingMessagesUseCase @Inject constructor(
    private val compressFileForChatUseCase: CompressFileForChatUseCase,
    private val monitorPendingMessagesByStateUseCase: MonitorPendingMessagesByStateUseCase,
    private val chatMessageRepository: ChatMessageRepository,
    private val updatePendingMessageUseCase: UpdatePendingMessageUseCase,
    private val getFileSizeFromUriPathUseCase: GetFileSizeFromUriPathUseCase,
) {

    /**
     * Invoke
     * @return a flow to track the compression progress
     */
    operator fun invoke(): Flow<ChatCompressionState> {
        val pathsToSizeAndProgress = mutableMapOf<UriPath, SizeAndProgress>()
        return channelFlow {
            monitorPendingMessagesByStateUseCase(PendingMessageState.COMPRESSING)
                .conflate()
                .collect { pendingMessageList ->
                    val semaphore = Semaphore(5) // to limit the parallel compressions
                    pendingMessageList
                        .groupBy { it.uriPath }
                        .map { (uriPath, pendingMessages) ->
                            //compress all received pending message's files in parallel, new pending messages will wait for this batch to be completed.
                            launch {
                                //if this was already compressed don't compress it again.
                                if (uriPath !in pathsToSizeAndProgress) {
                                    semaphore.withPermit {
                                        val original = uriPath
                                        val size =
                                            (getFileSizeFromUriPathUseCase(original) as? FileResult)?.sizeInBytes
                                                ?: 0
                                        pathsToSizeAndProgress[uriPath] = SizeAndProgress(
                                            size.coerceAtLeast(1L),
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
                                                chatMessageRepository.updatePendingMessagesCompressionProgress(
                                                    newProgress,
                                                    pendingMessages
                                                )
                                                pathsToSizeAndProgress[uriPath]?.copy(
                                                    progress = newProgress
                                                )?.let { progressUpdated ->
                                                    pathsToSizeAndProgress[uriPath] = progressUpdated
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
                                        // Once the file is compressed, let's set its state to ready to upload
                                        updatePendingMessageUseCase(
                                            updatePendingMessageRequests = pendingMessages.map { pendingMessage ->
                                                UpdatePendingMessageStateAndPathRequest(
                                                    pendingMessage.id,
                                                    PendingMessageState.READY_TO_UPLOAD,
                                                    (compressed?.absolutePath ?: original.value),
                                                )
                                            }.toTypedArray()
                                        )
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