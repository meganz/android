package mega.privacy.android.app.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.base.model.BaseState
import mega.privacy.android.domain.usecase.transfer.MonitorTransfersFinishedUseCase
import javax.inject.Inject

/**
 * View model for [mega.privacy.android.app.BaseActivity]
 *
 * @property state [BaseState]
 */
@HiltViewModel
class BaseViewModel @Inject constructor(
    private val monitorTransfersFinishedUseCase: MonitorTransfersFinishedUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(BaseState())
    val state: StateFlow<BaseState> = _state

    init {
        viewModelScope.launch {
            monitorTransfersFinishedUseCase().conflate().collect {
                _state.update { state -> state.copy(transfersFinished = it) }
            }
        }
    }

    /**
     * Sets transfersFinished in state as null.
     */
    fun onTransfersFinishedConsumed() =
        _state.update { state -> state.copy(transfersFinished = null) }
}