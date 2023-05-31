package mega.privacy.android.feature.sync.ui.newfolderpair

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SyncNewFolderViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(SyncNewFolderState())
    val state: StateFlow<SyncNewFolderState> = _state.asStateFlow()

    fun handleAction(action: SyncNewFolderAction) {
        // Will be implemented in the next MR
    }
}