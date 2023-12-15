package mega.privacy.android.app.presentation.settings.chat.imagequality

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.chat.imagequality.model.SettingsChatImageQualityState
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.usecase.GetChatImageQuality
import mega.privacy.android.domain.usecase.SetChatImageQuality
import timber.log.Timber
import javax.inject.Inject

/**
 * View model for [SettingsChatImageQualityFragment].
 */
@HiltViewModel
class SettingsChatImageQualityViewModel @Inject constructor(
    private val getChatImageQuality: GetChatImageQuality,
    private val setChatImageQuality: SetChatImageQuality,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsChatImageQualityState())
    val state: StateFlow<SettingsChatImageQualityState> = _state

    init {
        viewModelScope.launch {
            getChatImageQuality().collect { quality ->
                _state.update { state -> state.copy(selectedQuality = quality) }
            }
        }
    }

    /**
     * Sets a new chat image quality setting.
     *
     * @param quality The new quality.
     */
    fun setNewChatImageQuality(quality: ChatImageQuality) {
        viewModelScope.launch {
            runCatching { setChatImageQuality(quality) }
                .onFailure { Timber.e(it) }
        }
    }
}