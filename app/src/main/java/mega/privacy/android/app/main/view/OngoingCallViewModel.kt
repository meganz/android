package mega.privacy.android.app.main.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.chat.MonitorOngoingCallUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class OngoingCallViewModel @Inject constructor(
    private val monitorOngoingCallUseCase: MonitorOngoingCallUseCase,
    private val getThemeMode: GetThemeMode,
) : ViewModel() {
    private val _state = MutableStateFlow(OngoingCallUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorOngoingCallUseCase()
                .catch { Timber.e(it) }
                .collect { call ->
                    _state.update { it.copy(currentCall = call) }
                }
        }
        viewModelScope.launch {
            getThemeMode()
                .catch { Timber.e(it) }
                .collect { themeMode ->
                    _state.update { it.copy(themeMode = themeMode) }
                }
        }
    }

    fun setShow(show: Boolean) {
        _state.update { it.copy(isShown = show) }
    }

    fun isShowing(): Boolean {
        return state.value.isShown && state.value.currentCall != null
    }
}