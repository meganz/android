package mega.privacy.android.app.presentation.chat.groupInfo

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.presentation.chat.groupInfo.model.GroupInfoState
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.SetOpenInvite
import nz.mega.sdk.MegaChatRoom
import timber.log.Timber
import javax.inject.Inject

/**
 * GroupChatInfoActivity view model.
 *
 * @property setOpenInvite               [SetOpenInvite]
 * @property state                       Current view state as [GroupInfoState]
 */
@HiltViewModel
class GroupChatInfoViewModel @Inject constructor(
    private val setOpenInvite: SetOpenInvite,
    monitorConnectivity: MonitorConnectivity,
) : ViewModel() {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(GroupInfoState())

    /**
     * public UI State
     */
    val state: StateFlow<GroupInfoState> = _state

    private val isConnected =
        monitorConnectivity().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val openInviteChangeObserver =
        Observer<MegaChatRoom> { chat ->
            _state.update {
                it.copy(resultSetOpenInvite = chat.isOpenInvite)
            }
        }

    init {
        LiveEventBus.get(EventConstants.EVENT_CHAT_OPEN_INVITE, MegaChatRoom::class.java)
            .observeForever(openInviteChangeObserver)
    }

    override fun onCleared() {
        super.onCleared()
        LiveEventBus.get(EventConstants.EVENT_CHAT_OPEN_INVITE, MegaChatRoom::class.java)
            .removeObserver(openInviteChangeObserver)
    }

    /**
     * Allow add participants
     */
    fun onAllowAddParticipantsTap(chatId: Long) {
        if (isConnected.value) {
            viewModelScope.launch {
                runCatching {
                    setOpenInvite(chatId)
                }.onFailure { exception ->
                    Timber.e(exception)
                    _state.update { it.copy(error = R.string.general_text_error) }
                }.onSuccess { result ->
                    _state.update {
                        it.copy(resultSetOpenInvite = result)
                    }
                }
            }
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }
}