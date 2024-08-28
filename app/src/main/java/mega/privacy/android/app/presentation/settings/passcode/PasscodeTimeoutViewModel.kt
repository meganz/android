package mega.privacy.android.app.presentation.settings.passcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.passcode.mapper.PasscodeTimeoutMapper
import mega.privacy.android.app.presentation.settings.passcode.mapper.TimeoutOptionMapper
import mega.privacy.android.app.presentation.settings.passcode.model.PasscodeTimeoutUIState
import mega.privacy.android.app.presentation.settings.passcode.model.TimeoutOption
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeTimeOutUseCase
import mega.privacy.android.domain.usecase.passcode.SetPasscodeTimeoutUseCase
import javax.inject.Inject

/**
 * Passcode timeout view model
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PasscodeTimeoutViewModel @Inject constructor(
    private val monitorPasscodeTimeOutUseCase: MonitorPasscodeTimeOutUseCase,
    private val timeoutOptionMapper: TimeoutOptionMapper,
    private val setPasscodeTimeoutUseCase: SetPasscodeTimeoutUseCase,
    private val passcodeTimeoutMapper: PasscodeTimeoutMapper,
) : ViewModel() {

    private val _state =
        MutableStateFlow(PasscodeTimeoutUIState(options = timeOutOptions, currentOption = null))

    /**
     * State
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorPasscodeTimeOutUseCase()
                .mapLatest { timeout -> timeout?.let { timeoutOptionMapper(it) } }
                .collectLatest { uiOption ->
                    _state.update { it.copy(currentOption = uiOption) }
                }
        }
    }

    /**
     * On timeout selected
     *
     * @param option
     */
    fun onTimeoutSelected(option: TimeoutOption) {
        viewModelScope.launch {
            setPasscodeTimeoutUseCase(passcodeTimeoutMapper(option))
        }
    }
}

private val timeOutOptions = persistentListOf(
    TimeoutOption.Immediate,
    TimeoutOption.SecondsTimeSpan(5),
    TimeoutOption.SecondsTimeSpan(10),
    TimeoutOption.SecondsTimeSpan(30),
    TimeoutOption.MinutesTimeSpan(1),
    TimeoutOption.MinutesTimeSpan(2),
    TimeoutOption.MinutesTimeSpan(5),
)