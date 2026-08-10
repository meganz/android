package mega.privacy.android.app.presentation.chat.list.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.usecase.chat.MuteChatPushNotificationUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel backing [MuteChatDialog].
 */
@HiltViewModel
internal class MuteDialogViewModel @Inject constructor(
    private val muteChatPushNotificationUseCase: MuteChatPushNotificationUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MuteDialogUiState())
    val uiState: StateFlow<MuteDialogUiState> = _uiState.asStateFlow()

    fun muteOptionsFor(target: MuteTarget): List<ChatPushNotificationMuteOption> {
        val timed = listOf(
            ChatPushNotificationMuteOption.Mute30Minutes,
            ChatPushNotificationMuteOption.Mute1Hour,
            ChatPushNotificationMuteOption.Mute6Hours,
            ChatPushNotificationMuteOption.Mute24Hours,
        )
        val tail = when (target) {
            MuteTarget.Global -> if (TimeUtils.isUntilThisMorning()) {
                ChatPushNotificationMuteOption.MuteUntilThisMorning
            } else {
                ChatPushNotificationMuteOption.MuteUntilTomorrowMorning
            }

            is MuteTarget.Single, is MuteTarget.Multiple ->
                ChatPushNotificationMuteOption.MuteUntilTurnBackOn
        }
        return timed + tail
    }

    fun applyMute(
        target: MuteTarget,
        option: ChatPushNotificationMuteOption,
    ) {
        val chatIds: List<Long>? = when (target) {
            MuteTarget.Global -> null
            is MuteTarget.Single -> listOf(target.chatId)
            is MuteTarget.Multiple -> target.chatIds
        }
        viewModelScope.launch {
            runCatching {
                muteChatPushNotificationUseCase(chatIds, option)
            }.onSuccess {
                _uiState.update { it.copy(muteResultEvent = triggered(option)) }
            }.onFailure {
                Timber.e(it, "Failed to apply mute option $option")
            }
        }
    }

    fun onMuteResultEventConsumed() {
        _uiState.update { it.copy(muteResultEvent = consumed()) }
    }
}
