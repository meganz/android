package mega.privacy.android.feature.sync.ui.stopbackup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.ui.stopbackup.model.StopBackupState
import javax.inject.Inject

@HiltViewModel
internal class StopBackupViewModel @Inject constructor(
    private val monitorSelectedMegaFolderUseCase: MonitorSelectedMegaFolderUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(StopBackupState())
    val state: StateFlow<StopBackupState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorSelectedMegaFolderUseCase().collectLatest { folder ->
                _state.update { state ->
                    state.copy(selectedMegaFolder = folder)
                }
            }
        }
    }

    fun resetSelectedMegaFolder() = viewModelScope.launch {
        _state.update { state ->
            state.copy(selectedMegaFolder = null)
        }
    }
}