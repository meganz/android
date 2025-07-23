package mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_PLAY_VOICE_CLIP
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ProximitySensorState
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdateDoesNotExistInMessageUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodeUseCase
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * View model for voice clip message. This view model manages all visible voice clip messages
 * in a chat room.
 *
 * @property downloadNodeUseCase [DownloadNodeUseCase]
 * @property getCacheFileUseCase [GetCacheFileUseCase]
 */
@HiltViewModel
class VoiceClipMessageViewModel @Inject constructor(
    private val downloadNodeUseCase: DownloadNodeUseCase,
    private val getCacheFileUseCase: GetCacheFileUseCase,
    private val voiceClipPlayer: VoiceClipPlayer,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val updateDoesNotExistInMessageUseCase: UpdateDoesNotExistInMessageUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway
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
    fun addVoiceClip(message: VoiceClipMessage) = viewModelScope.launch {
        getMutableStateFlow(message.msgId)?.update { state ->
            val loadProgress = if (message.exists) state.loadProgress else null
            val timestamp = if (message.exists) state.timestamp else null
            state.copy(
                loadProgress = loadProgress,
                timestamp = timestamp,
                voiceClipMessage = message
            )
        }

        if (!message.exists ||
            message.status == ChatMessageStatus.SERVER_REJECTED ||
            message.status == ChatMessageStatus.SENDING_MANUAL ||
            message.status == ChatMessageStatus.SENDING ||
            message.duration.inWholeMilliseconds == 0L
        ) {
            return@launch
        }

        runCatching {
            val voiceClipFile: File =
                getCacheFileUseCase(CacheFolderManager.VOICE_CLIP_FOLDER, message.name) ?: run {
                    Timber.e("voice clip cache path is invalid for msgId(${message.msgId})")
                    updateDoesNotExists(message.msgId)
                    return@launch
                }

            if (isFileAlreadyDownloaded(voiceClipFile, message.size)) {
                getMutableStateFlow(message.msgId)?.update {
                    it.copy(
                        loadProgress = null,
                        timestamp = durationInSecondsTextMapper(message.duration)
                    )
                }
                return@launch
            }

            download(
                node = message.fileNode,
                destinationPath = "${voiceClipFile.parent}${File.separator}",
                msgId = message.msgId
            )
        }.onFailure {
            Timber.e(it)
            updateDoesNotExists(message.msgId)
        }
    }

    private fun isFileAlreadyDownloaded(file: File?, size: Long): Boolean =
        file?.let { it.exists() && it.length() == size } ?: false


    private suspend fun download(node: TypedNode, destinationPath: String, msgId: Long) {
        downloadNodeUseCase(
            node = node,
            destinationPath = destinationPath,
            appData = listOf(TransferAppData.VoiceClip),
            isHighPriority = true,
        )
            .onCompletion {
                getMutableStateFlow(msgId)?.let {
                    if (it.value.timestamp == null && it.value.loadProgress == null)
                        return@onCompletion

                    it.update { state ->
                        state.copy(
                            loadProgress = null,
                            timestamp = durationInSecondsTextMapper(state.voiceClipMessage?.duration),
                        )
                    }
                }
            }
            .catch {
                Timber.d(it, "Transfer TransferNotStarted msgId($msgId)")
                updateDoesNotExists(msgId)
            }
            .collect { downloadEvent ->
                if (downloadEvent.isFinishedWithErrorEvent()) {
                    updateDoesNotExists(msgId)
                } else {
                    getMutableStateFlow(msgId)?.update {
                        it.copy(loadProgress = downloadEvent.transfer.progress)
                    }
                }
            }
    }

    private fun updateDoesNotExists(msgId: Long) {
        getMutableStateFlow(msgId)?.update {
            it.voiceClipMessage?.let {
                viewModelScope.launch {
                    updateDoesNotExistInMessageUseCase(it.chatId, msgId)
                }
            }
            it.copy(
                timestamp = null,
                loadProgress = null,
            )
        }
    }

    /**
     * Handle when user seeks the voice clip message a new position.
     *
     * @param progress progress is a value between 0 and 1.
     * @param msgId message id of the voice clip message.
     */
    fun onSeek(progress: Float, msgId: Long) = viewModelScope.launch {
        runCatching {
            val uiState = getMutableStateFlow(msgId)
            val durationInMilliseconds =
                uiState?.value?.voiceClipMessage?.duration?.inWholeMilliseconds
                    ?: throw IllegalStateException("Voice clip message not found for msgId($msgId)")
            voiceClipPlayer.seekTo(msgId, (durationInMilliseconds * progress).toInt())

            uiState.update {
                it.copy(
                    playProgress = Progress(progress),
                    timestamp = durationInSecondsTextMapper(
                        (durationInMilliseconds * progress).toLong().milliseconds
                    )
                )
            }
        }.onFailure { Timber.e(it) }
    }

    /**
     * Handle when user clicks the button in voice clip to play or pause.
     *
     * @param msgId of the clicked voice clip message.
     */
    fun onPlayOrPauseClicked(msgId: Long) = viewModelScope.launch {
        runCatching {
            Timber.d("onPlayOrPauseClicked msgId($msgId)")
            if (voiceClipPlayer.isPlaying(msgId)) {
                pauseVoiceClip(msgId)
            } else {
                getMutableStateFlow(msgId)?.update {
                    it.copy(
                        isPaused = false,
                    )
                }
                val voiceClipFile = getVoiceClipFile(msgId) ?: run {
                    Timber.e("voice clip cache path is invalid for msgId(${msgId})")
                    updateDoesNotExists(msgId)
                    return@launch
                }

                pauseOngoingVoiceClip()

                val duration =
                    getMutableStateFlow(msgId)?.value?.voiceClipMessage?.duration?.inWholeMilliseconds
                        ?: 0L
                val currentProgress =
                    getMutableStateFlow(msgId)?.value?.playProgress?.floatValue ?: 0f
                val currentPos = (duration * currentProgress).toInt()
                startProximitySensor(msgId = msgId)

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
            updateDoesNotExists(msgId)
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
        getMutableStateFlow(msgId)?.apply {
            if (value.isPaused) {
                return
            }
        }

        val timestamp = durationInSecondsTextMapper(state.pos.milliseconds)
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
        stopProximitySensor(msgId)
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
                Timber.d("Pause ongoing voice clip, msgId(${it.msgId})")
                voiceClipPlayer.pause(it.msgId)
                getMutableStateFlow(it.msgId)?.update { state ->
                    state.copy(
                        isPlaying = false,
                    )
                }
            }
    }

    private fun pauseVoiceClip(msgId: Long) {
        Timber.d("Pause voice clip, msgId($msgId)")
        stopProximitySensor(msgId)
        voiceClipPlayer.pause(msgId)
        getMutableStateFlow(msgId)?.update {
            it.copy(isPlaying = false, isPaused = true)
        }
    }

    /**
     *  Create RTC Audio Manager and start proximity sensor
     */
    fun startProximitySensor(msgId: Long) {
        Timber.d("Start proximity sensor")
        rtcAudioManagerGateway.createOrUpdateAudioManager(
            isSpeakerOn = true,
            type = AUDIO_MANAGER_PLAY_VOICE_CLIP
        )

        rtcAudioManagerGateway.startProximitySensor { isNear ->
            if (!isNear) {
                if (getMutableStateFlow(msgId)?.value?.proximitySensorState == ProximitySensorState.Near &&
                    voiceClipPlayer.isPlaying(msgId)
                ) {
                    pauseVoiceClip(msgId)
                }
            }

            getMutableStateFlow(msgId)?.update {
                it.copy(proximitySensorState = if (isNear) ProximitySensorState.Near else ProximitySensorState.Far)
            }
        }
    }

    /**
     * Unregister proximity sensor and remove RTC Audio manager
     *
     * @param msgId
     */
    fun stopProximitySensor(msgId: Long) {
        Timber.d("Stop proximity sensor")
        getMutableStateFlow(msgId)?.update {
            it.copy(proximitySensorState = ProximitySensorState.Unknown)
        }
        rtcAudioManagerGateway.unregisterProximitySensor()
        rtcAudioManagerGateway.removeRTCAudioManager()
    }

    private fun getMutableStateFlow(msgId: Long) = _uiStateFlowMap[msgId]
}
