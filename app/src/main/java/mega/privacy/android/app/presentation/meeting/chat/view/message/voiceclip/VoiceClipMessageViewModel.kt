package mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.DownloadNodesEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodesUseCase
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * View model for voice clip message. This view model manages all visible voice clip messages
 * in a chat room.
 *
 * @property downloadNodesUseCase [DownloadNodesUseCase]
 * @property getChatFileUseCase [GetChatFileUseCase]
 * @property getCacheFileUseCase [GetCacheFileUseCase]
 */
@HiltViewModel
class VoiceClipMessageViewModel @Inject constructor(
    private val downloadNodesUseCase: DownloadNodesUseCase,
    private val getChatFileUseCase: GetChatFileUseCase,
    private val getCacheFileUseCase: GetCacheFileUseCase,
) : ViewModel() {

    // key: msgId, value: ui state flow
    private val _uiStateFlowMap: ConcurrentHashMap<Long, MutableStateFlow<VoiceClipMessageUiState>> =
        ConcurrentHashMap()

    /**
     * Get the [StateFlow] for a given voice clip message which has [msgId]
     */
    fun getUiStateFlow(msgId: Long): StateFlow<VoiceClipMessageUiState> =
        _uiStateFlowMap[msgId]
            ?: MutableStateFlow(VoiceClipMessageUiState()).also { _uiStateFlowMap[msgId] = it }

    /**
     * Add visible voice clip message, so view model can start handling it.
     */
    fun addVoiceClip(message: VoiceClipMessage, chatId: Long) = viewModelScope.launch {

        _uiStateFlowMap[message.msgId]?.update {
            it.copy(voiceClipMessage = message)
        }

        val hasError =
            message.status == ChatMessageStatus.SERVER_REJECTED ||
                    message.status == ChatMessageStatus.SENDING_MANUAL ||
                    message.status == ChatMessageStatus.SENDING ||
                    message.duration == 0
        if (hasError) {
            updateUiWithError(message.msgId)
            return@launch
        }

        val voiceClipFile: File =
            getCacheFileUseCase(CacheFolderManager.VOICE_CLIP_FOLDER, message.name) ?: run {
                Timber.e("voice clip cache path is invalid for msgId(${message.msgId})")
                updateUiWithError(message.msgId)
                return@launch
            }

        if (isFileAlreadyDownloaded(voiceClipFile, message.size)) {
            _uiStateFlowMap[message.msgId]?.update {
                it.copy(
                    loadProgress = null,
                    timestamp = secondsToDuration(message.duration)
                )
            }
            return@launch
        }

        runCatching {
            voiceClipFile.parent?.let { destination ->
                getChatFileUseCase(chatId = chatId, messageId = message.msgId)?.let { node ->
                    download(
                        node = node,
                        destinationPath = destination,
                        msgId = message.msgId
                    )
                }
                    ?: throw IllegalStateException("getChatFileUseCase return null for msgId(${message.msgId})")
            }
                ?: throw IllegalStateException("voice clip cache path is invalid for msgId(${message.msgId})")
        }.onFailure {
            Timber.e(it)
            updateUiWithError(message.msgId)
        }
    }

    private fun isFileAlreadyDownloaded(file: File?, size: Long): Boolean =
        file?.let { it.exists() && it.length() == size } ?: false


    private suspend fun download(node: TypedNode, destinationPath: String, msgId: Long) {
        downloadNodesUseCase(
            nodes = listOf(node),
            destinationPath = destinationPath,
            appData = TransferAppData.VoiceClip,
            isHighPriority = true,
        ).collect { downloadEvent ->
            when (downloadEvent) {
                is DownloadNodesEvent.TransferNotStarted -> {
                    Timber.d("Transfer TransferNotStarted msgId($msgId) (${downloadEvent.exception})")
                    updateUiWithError(msgId)
                }

                is DownloadNodesEvent.TransferFinishedProcessing -> {
                    _uiStateFlowMap[msgId]?.update {
                        it.copy(
                            loadProgress = null,
                            timestamp = secondsToDuration(it.voiceClipMessage?.duration ?: 0),
                        )
                    }
                }

                is DownloadNodesEvent.SingleTransferEvent -> {
                    with(downloadEvent.transferEvent.transfer) {
                        Timber.d(
                            "DDD msgId($msgId) Transfer SingleTransferEvent" +
                                    " event($transferredBytes/$totalBytes)"
                        )
                    }
                }

                is DownloadNodesEvent.FinishProcessingTransfers -> {
                }

                is DownloadNodesEvent.NotSufficientSpace -> {
                    updateUiWithError(msgId)
                }
            }
        }
    }

    private fun updateUiWithError(msgId: Long) {
        _uiStateFlowMap[msgId]?.update {
            it.copy(isError = true)
        }
    }

    /**
     * Handle when user clicks the button in voice clip to play or pause.
     *
     * @param msgId
     */
    fun onPlayOrPauseClicked(msgId: Long) {
        Timber.d("TODO onPlayOrPauseClicked msgId = $msgId")
    }

    private fun secondsToDuration(seconds: Int): String {
        val hours = (seconds / (60 * 60))
        val minutes = (seconds % (60 * 60)) / 60
        val sec = (seconds % (60 * 60) % 60)
        return when {
            hours > 0 -> "%02d:%02d:%02d".format(hours, minutes, sec)
            else -> "%02d:%02d".format(minutes, sec)
        }
    }
}