package mega.privacy.android.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.HasIncomingCall
import mega.privacy.android.app.domain.usecase.MonitorChatNotificationCount
import mega.privacy.android.app.presentation.home.model.HomeState
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val monitorChatNotificationCount: MonitorChatNotificationCount,
    private val hasIncomingCall: HasIncomingCall,
) : ViewModel() {
    private val state = MutableStateFlow(HomeState(displayCallBadge = false))
    val homeState: StateFlow<HomeState> = state
    val homeStateLiveData = state.asLiveData()

    init {
        viewModelScope.launch {
            combine(
                monitorChatNotificationCount(),
                hasIncomingCall(),
            ) { count, inCall ->
                { state: HomeState ->
                    state.copy(
                        unreadNotificationsCount = count,
                        displayChatCount = (count > 0) && !inCall,
                        displayCallBadge = inCall
                    )
                }
            }.collect {
                state.update(it)
            }
        }
    }

}