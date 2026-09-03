package mega.privacy.android.app.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.base.model.BaseState
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.business.MonitorBusinessAccountExpiredUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View model for [mega.privacy.android.app.BaseActivity]
 *
 * @property state [BaseState]
 */
@HiltViewModel
class BaseViewModel @Inject constructor(
    private val monitorAccountBlockedUseCase: MonitorAccountBlockedUseCase,
    private var monitorBusinessAccountExpiredUseCase: MonitorBusinessAccountExpiredUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(BaseState())
    val state: StateFlow<BaseState> = _state

    init {
        viewModelScope.launch {
            monitorAccountBlockedUseCase().collect {
                _state.update { state -> state.copy(accountBlockedEvent = it) }
            }
        }
        viewModelScope.launch {
            monitorBusinessAccountExpiredUseCase().collect {
                _state.update { state -> state.copy(showExpiredBusinessAlert = true) }
            }
        }
        viewModelScope.launch {
            monitorAccountDetailUseCase()
                .catch { Timber.e(it, "Exception monitoring account details") }
                .collect { accountDetail ->
                    _state.update { state ->
                        state.copy(accountType = accountDetail.levelDetail?.accountType)
                    }
                }
        }
    }

    /**
     * Sets accountBlockedEvent in state as null.
     */
    fun onAccountBlockedConsumed() =
        _state.update { state -> state.copy(accountBlockedEvent = null) }

    /**
     * Sets showExpiredBusinessAlert in state as false
     */
    fun onShowExpiredBusinessAlertConsumed() =
        _state.update { state -> state.copy(showExpiredBusinessAlert = false) }
}