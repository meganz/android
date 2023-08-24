package mega.privacy.android.app.presentation.security.check

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.security.check.model.PasscodeCheckState
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeLockStateUseCase
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class PasscodeCheckViewModel @Inject constructor(
    private val monitorPasscodeLockStateUseCase: MonitorPasscodeLockStateUseCase,
) : ViewModel() {

    private val _state: MutableStateFlow<PasscodeCheckState> =
        MutableStateFlow(PasscodeCheckState.Loading)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                monitorPasscodeLockStateUseCase()
                    .mapLatest { locked ->
                        if (locked) PasscodeCheckState.Locked else PasscodeCheckState.UnLocked
                    }.collectLatest {
                        _state.emit(it)
                    }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}