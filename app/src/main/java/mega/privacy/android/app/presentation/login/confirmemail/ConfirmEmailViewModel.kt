package mega.privacy.android.app.presentation.login.confirmemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.login.confirmemail.model.ConfirmEmailState
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.domain.usecase.login.MonitorAccountUpdateUseCase
import javax.inject.Inject

/**
 * View Model for [ConfirmEmailFragment]
 *
 * @property state View state as [ConfirmEmailState]
 */
@HiltViewModel
class ConfirmEmailViewModel @Inject constructor(
    private val monitorAccountUpdateUseCase: MonitorAccountUpdateUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ConfirmEmailState())
    val state: StateFlow<ConfirmEmailState> = _state

    init {
        viewModelScope.launch {
            monitorAccountUpdateUseCase().collectLatest {
                _state.update { state -> state.copy(isPendingToShowFragment = LoginFragmentType.Login) }
            }
        }
    }

    /**
     * Update state with isPendingToShowFragment as null.
     */
    fun isPendingToShowFragmentConsumed() {
        _state.update { state -> state.copy(isPendingToShowFragment = null) }
    }
}