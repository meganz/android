package mega.privacy.android.app.presentation.security.check

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mega.privacy.android.app.presentation.security.check.model.PasscodeCheckState
import javax.inject.Inject

@HiltViewModel
internal class PasscodeCheckViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(PasscodeCheckState.Loading)
    val state: StateFlow<PasscodeCheckState> = _state.asStateFlow()
}