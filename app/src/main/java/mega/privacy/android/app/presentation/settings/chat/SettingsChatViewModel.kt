package mega.privacy.android.app.presentation.settings.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.chat.model.SettingsChatState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetChatImageQuality
import mega.privacy.android.domain.usecase.MonitorChatSignalPresenceUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.SignalChatPresenceActivity
import mega.privacy.android.domain.usecase.chat.GetChatPresenceConfigUseCase
import mega.privacy.android.domain.usecase.chat.SetCurrentUserStatusUseCase
import mega.privacy.android.domain.usecase.chat.SetPresenceAutoAwayUseCase
import mega.privacy.android.domain.usecase.chat.SetPresencePersistUseCase
import mega.privacy.android.domain.usecase.chat.link.EnableRichPreviewUseCase
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
    private val monitorChatSignalPresenceUseCase: MonitorChatSignalPresenceUseCase,
    private val getChatPresenceConfigUseCase: GetChatPresenceConfigUseCase,
    private val setCurrentUserStatusUseCase: SetCurrentUserStatusUseCase,
    private val setPresenceAutoAwayUseCase: SetPresenceAutoAwayUseCase,
    private val setPresencePersistUseCase: SetPresencePersistUseCase,
    private val signalChatPresenceActivity: SignalChatPresenceActivity,
    private val enableRichPreviewUseCase: EnableRichPreviewUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsChatState(
            chatPresenceConfig = null,
            rootNodeExists = false,
        )
    )
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
        viewModelScope.launch {
            monitorChatSignalPresenceUseCase()
                .onStart { emit(Unit) }
                .catch { Timber.e(it, "Error monitoring chat signal presence") }
                .collect {
                    runCatching { getChatPresenceConfigUseCase() }
                        .onSuccess { config ->
                            _state.update { state -> state.copy(chatPresenceConfig = config) }
                        }
                        .onFailure { Timber.e(it, "Error getting chat presence config") }
                }
        }
        viewModelScope.launch {
            runCatching { rootNodeExistsUseCase() }
                .onSuccess { exists ->
                    _state.update { state -> state.copy(rootNodeExists = exists) }
                }
                .onFailure { Timber.e(it, "Error checking if root node exists") }
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

    /**
     * Signals that there is user activity, so the away status is not set automatically.
     */
    fun signalChatPresence() {
        viewModelScope.launch {
            runCatching { signalChatPresenceActivity() }
                .onFailure { Timber.e(it, "Failed to signal chat presence activity") }
        }
    }

    /**
     * Sets the current user chat status.
     *
     * @param statusValue the raw status list preference value.
     */
    fun setUserStatus(statusValue: Int) {
        val status = statusValue.toUserChatStatus() ?: return
        viewModelScope.launch {
            runCatching { setCurrentUserStatusUseCase(status) }
                .onFailure { Timber.e(it, "Failed to set user chat status") }
        }
    }

    /**
     * Enables the auto-away option with the default timeout, or disables it.
     *
     * @param enabled whether auto-away should be enabled.
     */
    fun setAutoAwayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                if (enabled) {
                    setPresenceAutoAwayUseCase(true, DEFAULT_AUTO_AWAY_TIMEOUT_SECONDS)
                } else {
                    setPresenceAutoAwayUseCase(false, 0)
                }
            }.onFailure { Timber.e(it, "Failed to set presence auto-away") }
        }
    }

    /**
     * Enables or disables persisting the online status after going offline.
     *
     * @param enabled whether the online status should persist.
     */
    fun setPresencePersist(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { setPresencePersistUseCase(enabled) }
                .onFailure { Timber.e(it, "Failed to set presence persist") }
        }
    }

    /**
     * Enables or disables rich link previews.
     *
     * @param enabled whether rich link previews should be enabled.
     */
    fun enableRichLinkPreview(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { enableRichPreviewUseCase(enabled) }
                .onFailure { Timber.e(it, "Failed to enable rich link previews") }
        }
    }

    companion object {
        private const val DEFAULT_AUTO_AWAY_TIMEOUT_SECONDS = 300
    }
}
