package mega.privacy.android.app.presentation.settings.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.chat.model.SettingsChatState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetChatImageQuality
import mega.privacy.android.domain.usecase.chat.link.MonitorRichLinkPreviewConfigUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.GetChatSettingsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorChatSettingsUseCase
import mega.privacy.android.domain.usecase.setting.SetChatSettingsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View model for [SettingsChatFragment].
 */
@HiltViewModel
class SettingsChatViewModel @Inject constructor(
    private val getChatImageQuality: GetChatImageQuality,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val monitorRichLinkPreviewConfigUseCase: MonitorRichLinkPreviewConfigUseCase,
    private val monitorChatSettingsUseCase: MonitorChatSettingsUseCase,
    private val getChatSettingsUseCase: GetChatSettingsUseCase,
    private val setChatSettingsUseCase: SetChatSettingsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsChatState())
    val state: StateFlow<SettingsChatState> = _state

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent = monitorConnectivityUseCase()

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    init {
        viewModelScope.launch(ioDispatcher) {
            getChatImageQuality().map { quality ->
                { state: SettingsChatState -> state.copy(imageQuality = quality) }
            }.collect {
                _state.update(it)
            }
        }
        viewModelScope.launch {
            monitorRichLinkPreviewConfigUseCase()
                .catch { Timber.e(it, "Error monitoring rich link preview config") }
                .collect {
                    _state.update { state ->
                        state.copy(isRichLinkEnabled = it.isRichLinkEnabled)
                    }
                }
        }
        viewModelScope.launch {
            monitorChatSettingsUseCase()
                .catch { Timber.e(it, "Error monitoring chat settings") }
                .collect { chatSettings ->
                    _state.update { state ->
                        state.copy(
                            chatVideoQuality = chatSettings?.videoQuality?.toIntOrNull()
                                ?: VideoQuality.MEDIUM.value
                        )
                    }
                }
        }
    }

    /**
     * Persists the chat video (attachment) upload quality through the chat settings use cases.
     *
     * @param quality the new [mega.privacy.android.domain.entity.VideoQuality] value.
     */
    fun setChatVideoQuality(quality: Int) {
        viewModelScope.launch {
            runCatching {
                val current = getChatSettingsUseCase() ?: ChatSettings()
                setChatSettingsUseCase(current.copy(videoQuality = quality.toString()))
            }.onFailure { Timber.e(it, "Failed to set chat video quality") }
        }
    }
}
