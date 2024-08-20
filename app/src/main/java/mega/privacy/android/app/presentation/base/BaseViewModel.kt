package mega.privacy.android.app.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.base.model.BaseState
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.business.MonitorBusinessAccountExpiredUseCase
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
) : ViewModel() {

    private val _state = MutableStateFlow(BaseState())
    val state: StateFlow<BaseState> = _state

    init {
        viewModelScope.launch {
            monitorAccountBlockedUseCase().collect {
                _state.update { state -> state.copy(accountBlockedDetail = it) }
            }
        }
        viewModelScope.launch {
            monitorBusinessAccountExpiredUseCase().collect {
                _state.update { state -> state.copy(showExpiredBusinessAlert = true) }
            }
        }
    }

    /**
     * Sets accountBlockedDetail in state as null.
     */
    fun onAccountBlockedConsumed() =
        _state.update { state -> state.copy(accountBlockedDetail = null) }

    /**
     * Sets showExpiredBusinessAlert in state as false
     */
    fun onShowExpiredBusinessAlertConsumed() =
        _state.update { state -> state.copy(showExpiredBusinessAlert = false) }
}