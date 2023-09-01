package mega.privacy.android.app.presentation.chat.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetDeviceCurrentTimeUseCase
import mega.privacy.android.domain.usecase.GetLastContactPermissionDismissedTime
import mega.privacy.android.domain.usecase.SetLastContactPermissionDismissedTime
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import javax.inject.Inject

/**
 * viewModel for recent chat screen
 *
 * @property setLastContactPermissionDismissedTime use case for setting the time when contact permission popup has been last dismissed
 * @property getLastContactPermissionDismissedTime use case for getting the time when contact permission popup has been last dismissed
 * @property ioDispatcher
 * @property getDeviceCurrentTimeUseCase get the current time
 * @property monitorConnectivityUseCase to monitor internet connectivity
 */
@HiltViewModel
class RecentChatsViewModel @Inject constructor(
    private val setLastContactPermissionDismissedTime: SetLastContactPermissionDismissedTime,
    private val getLastContactPermissionDismissedTime: GetLastContactPermissionDismissedTime,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getDeviceCurrentTimeUseCase: GetDeviceCurrentTimeUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(RecentChatsState())

    /**
     * state of Recent Chat View
     */
    val state = _state.asStateFlow()

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    init {
        viewModelScope.launch(ioDispatcher) {
            getLastContactPermissionDismissedTime()
                .flowOn(ioDispatcher)
                .collect { lastDismissedTime ->
                    handleLastDismissedTime(getDeviceCurrentTimeUseCase(), lastDismissedTime)
                }
        }
    }

    /**
     * set last dismiss request contact
     */
    fun setLastDismissedRequestContactTime() {
        viewModelScope.launch(ioDispatcher) {
            setLastContactPermissionDismissedTime(getDeviceCurrentTimeUseCase())
        }
    }

    private fun handleLastDismissedTime(currentTime: Long, lastDismissTime: Long) {
        _state.value =
            _state.value.copy(shouldShowRequestContactAccess = currentTime - lastDismissTime > DURATION_TO_SHOW_REQUEST_ACCESS_AGAIN)
    }

    companion object {
        /**
         * Duration to show request contact access again
         */
        const val DURATION_TO_SHOW_REQUEST_ACCESS_AGAIN = Constants.SECONDS_IN_WEEK * 1000L
    }
}
