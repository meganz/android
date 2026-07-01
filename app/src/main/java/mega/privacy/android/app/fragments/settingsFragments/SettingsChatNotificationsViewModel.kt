package mega.privacy.android.app.fragments.settingsFragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.entity.settings.ChatSettings.Companion.VIBRATION_OFF
import mega.privacy.android.domain.entity.settings.ChatSettings.Companion.VIBRATION_ON
import mega.privacy.android.domain.usecase.setting.GetChatSettingsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorChatSettingsUseCase
import mega.privacy.android.domain.usecase.setting.SetChatSettingsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for [SettingsChatNotificationsFragment]. Exposes the chat notification settings as
 * [SettingsChatNotificationsUiState] and persists user changes through the chat settings use cases.
 */
@HiltViewModel
class SettingsChatNotificationsViewModel @Inject constructor(
    private val monitorChatSettingsUseCase: MonitorChatSettingsUseCase,
    private val getChatSettingsUseCase: GetChatSettingsUseCase,
    private val setChatSettingsUseCase: SetChatSettingsUseCase,
) : ViewModel() {

    /**
     * Flow of [SettingsChatNotificationsUiState].
     */
    val uiState: StateFlow<SettingsChatNotificationsUiState> by lazy(LazyThreadSafetyMode.NONE) {
        monitorChatSettingsUseCase()
            .map { chatSettings ->
                SettingsChatNotificationsUiState(
                    notificationsSound = chatSettings?.notificationsSound,
                    isVibrationEnabled = chatSettings?.vibrationEnabled?.toBoolean() ?: true,
                )
            }
            .catch { Timber.e(it, "Failed to monitor chat settings") }
            .asUiStateFlow(viewModelScope, SettingsChatNotificationsUiState())
    }

    /**
     * Toggles the chat notification vibration setting.
     */
    fun toggleVibration() {
        viewModelScope.launch {
            runCatching {
                val current = getChatSettingsUseCase() ?: ChatSettings()
                val vibrationEnabled =
                    if (current.vibrationEnabled.toBoolean()) VIBRATION_OFF else VIBRATION_ON
                setChatSettingsUseCase(current.copy(vibrationEnabled = vibrationEnabled))
            }.onFailure { Timber.e(it, "Failed to toggle chat vibration") }
        }
    }

    /**
     * Sets the chat notification sound.
     *
     * @param sound the new notification sound value.
     */
    fun setNotificationSound(sound: String?) {
        viewModelScope.launch {
            runCatching {
                val current = getChatSettingsUseCase() ?: ChatSettings()
                setChatSettingsUseCase(current.copy(notificationsSound = sound.orEmpty()))
            }.onFailure { Timber.e(it, "Failed to set chat notification sound") }
        }
    }
}
