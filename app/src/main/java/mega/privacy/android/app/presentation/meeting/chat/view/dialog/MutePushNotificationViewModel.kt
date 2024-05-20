package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.usecase.chat.GetChatMuteOptionListUseCase
import javax.inject.Inject

@HiltViewModel
class MutePushNotificationViewModel @Inject constructor(
    private val getChatMuteOptionListUseCase: GetChatMuteOptionListUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(emptyList<ChatPushNotificationMuteOption>())
    internal val mutePushNotificationUiState = _state.asStateFlow()

    init {
        loadValues()
    }

    /**
     * Show the dialog of selecting chat mute options
     */
    private fun loadValues() {
        viewModelScope.launch {
            _state.value = runCatching {
                getChatMuteOptionListUseCase()
            }.getOrDefault(emptyList())
        }
    }
}