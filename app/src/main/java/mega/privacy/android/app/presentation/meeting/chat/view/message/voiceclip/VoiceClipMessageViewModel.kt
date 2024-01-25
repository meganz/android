package mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.meeting.chat.mapper.DurationTextMapper
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodesUseCase
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

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
    private val voiceClipPlayer: VoiceClipPlayer,
    private val durationTextMapper: DurationTextMapper,
) : ViewModel() {

    // key: msgId, value: ui state flow
    private val _uiStateFlowMap: ConcurrentHashMap<Long, MutableStateFlow<VoiceClipMessageUiState>> =
        ConcurrentHashMap()

    /**
     * Clear resources when view model is destroyed
     */
    override fun onCleared() {
        voiceClipPlayer.releaseAll()
        super.onCleared()
    }

    /**
     * Get the [StateFlow] for a given voice clip message which has [msgId]
     */
    fun getUiStateFlow(msgId: Long): StateFlow<VoiceClipMessageUiState> =
        getMutableStateFlow(msgId)
            ?: MutableStateFlow(VoiceClipMessageUiState()).also { _uiStateFlowMap[msgId] = it }

    /**
     * Add visible voice clip message, so view model can start handling it.
     */
    fun addVoiceClip(message: VoiceClipMessage, chatId: Long) = viewModelScope.launch {

        getMutableStateFlow(message.msgId)?.update {
            it.copy(voiceClipMessage = message)
        }

        val hasError =
            message.status == ChatMessageStatus.SERVER_REJECTED ||
                    message.status == ChatMessageStatus.SENDING_MANUAL ||
                    message.status == ChatMessageStatus.SENDING ||
                    message.duration.inWholeMilliseconds == 0L
        if (hasError) {
            updateUiToErrorState(message.msgId)
            return@launch
        }
        runCatching {
            val voiceClipFile: File =
                getCacheFileUseCase(CacheFolderManager.VOICE_CLIP_FOLDER, message.name) ?: run {
                    Timber.e("voice clip cache path is invalid for msgId(${message.msgId})")
                    updateUiToErrorState(message.msgId)
                    return@launch
                }

            if (isFileAlreadyDownloaded(voiceClipFile, message.size)) {
                getMutableStateFlow(message.msgId)?.update {
                    it.copy(
                        loadProgress = null,
                        timestamp = durationTextMapper(message.duration, DurationUnit.MILLISECONDS)
                    )
                }
                return@launch
            }

            getChatFileUseCase(chatId = chatId, messageId = message.msgId)?.let { node ->
                download(
                    node = node,
                    destinationPath = "${voiceClipFile.parent}${File.separator}",
                    msgId = message.msgId
                )
            }
                ?: throw IllegalStateException("getChatFileUseCase return null for msgId(${message.msgId})")
        }.onFailure {
            Timber.e(it)
            updateUiToErrorState(message.msgId)
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
        )
            .onCompletion {
                getMutableStateFlow(msgId)?.update {
                    it.copy(
                        loadProgress = null,
                        timestamp = durationTextMapper(
                            it.voiceClipMessage?.duration,
                            DurationUnit.MILLISECONDS
                        ),
                    )
                }
            }
            .collect { downloadEvent ->
                when (downloadEvent) {
                    is MultiTransferEvent.TransferNotStarted<*> -> {
                        Timber.d("Transfer TransferNotStarted msgId($msgId) (${downloadEvent.exception})")
                        updateUiToErrorState(msgId)
                    }

                    is MultiTransferEvent.SingleTransferEvent -> {
                        getMutableStateFlow(msgId)?.update {
                            it.copy(
                                loadProgress = downloadEvent.overallProgress
                            )
                        }
                    }

                    is MultiTransferEvent.ScanningFoldersFinished -> {
                    }

                    is MultiTransferEvent.InsufficientSpace -> {
                        updateUiToErrorState(msgId)
                    }
                }
            }
    }

    private fun updateUiToErrorState(msgId: Long) {
        getMutableStateFlow(msgId)?.update {
            it.copy(isError = true)
        }
    }

    /**
     * Handle when user clicks the button in voice clip to play or pause.
     *
     * @param msgId of the clicked voice clip message.
     */
    fun onPlayOrPauseClicked(msgId: Long) = viewModelScope.launch {
        runCatching {
            if (voiceClipPlayer.isPlaying(msgId)) {
                pauseVoiceClip(msgId)
            } else {
                val voiceClipFile = getVoiceClipFile(msgId) ?: run {
                    Timber.e("voice clip cache path is invalid for msgId(${msgId})")
                    updateUiToErrorState(msgId)
                    return@launch
                }

                pauseOngoingVoiceClip()

                val currentPos = voiceClipPlayer.getCurrentPosition(msgId)
                voiceClipPlayer.play(
                    key = msgId,
                    path = voiceClipFile.absolutePath,
                    pos = currentPos
                ).collect { state ->
                    when (state) {
                        is VoiceClipPlayState.Playing -> {
                            updateUiToPlayingState(state, msgId)
                        }

                        is VoiceClipPlayState.Completed -> {
                            updateUiToNormalState(msgId)
                        }

                        is VoiceClipPlayState.Error -> {
                            Timber.e(" VoiceClipPlayState.Error(${state.error})")
                            updateUiToNormalState(msgId)
                        }

                        else -> {
                            Timber.d("VoiceClipPlayer Other state($state)")
                        }
                    }
                }
            }
        }.onFailure {
            Timber.e(it)
            updateUiToErrorState(msgId)
        }
    }

    private fun getVoiceClipFile(msgId: Long) =
        getMutableStateFlow(msgId)?.value?.voiceClipMessage?.name?.let { name ->
            getCacheFileUseCase(CacheFolderManager.VOICE_CLIP_FOLDER, name)
        }?.takeIf {
            it.exists()
        }


    private fun updateUiToPlayingState(
        state: VoiceClipPlayState.Playing,
        msgId: Long,
    ) {
        val timestamp = durationTextMapper(
            state.pos.milliseconds,
            DurationUnit.MILLISECONDS
        )
        val playProgress =
            getUiStateFlow(msgId).value.voiceClipMessage?.duration?.let { dur ->
                Progress(state.pos, dur.inWholeMilliseconds)
            }
        getMutableStateFlow(msgId)?.update {
            it.copy(
                isPlaying = true,
                playProgress = playProgress,
                timestamp = timestamp,
            )
        }
    }

    private fun updateUiToNormalState(msgId: Long) {
        getMutableStateFlow(msgId)?.update {
            it.copy(
                isPlaying = false,
                playProgress = null,
            )
        }
    }

    private fun pauseOngoingVoiceClip() {
        _uiStateFlowMap.values
            .filter { it.value.isPlaying }
            .map { it.value.voiceClipMessage }
            .firstOrNull()?.let {
                voiceClipPlayer.pause(it.msgId)
                getMutableStateFlow(it.msgId)?.update { state ->
                    state.copy(
                        isPlaying = false,
                    )
                }
            }
    }

    private fun pauseVoiceClip(msgId: Long) {
        voiceClipPlayer.pause(msgId)
        getMutableStateFlow(msgId)?.update {
            it.copy(isPlaying = false)
        }
    }

    private fun getMutableStateFlow(msgId: Long) = _uiStateFlowMap[msgId]
}
