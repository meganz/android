package mega.privacy.android.feature.sync.ui.newfolderpair

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.file.GetExternalPathByContentUriUseCase
import javax.inject.Inject

@HiltViewModel
internal class SyncNewFolderViewModel @Inject constructor(
    private val getExternalPathByContentUriUseCase: GetExternalPathByContentUriUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncNewFolderState())
    val state: StateFlow<SyncNewFolderState> = _state.asStateFlow()

    fun handleAction(action: SyncNewFolderAction) {
        when (action) {
            is SyncNewFolderAction.RemoteFolderSelected -> {
                _state.update { state ->
                    state.copy(selectedMegaFolder = action.remoteFolder)
                }
            }

            is SyncNewFolderAction.LocalFolderSelected -> {
                viewModelScope.launch {
                    getExternalPathByContentUriUseCase(action.path.toString())?.let { path ->
                        _state.update { state ->
                            state.copy(selectedLocalFolder = path)
                        }
                    }
                }
            }

            is SyncNewFolderAction.FolderNameChanged -> {
                _state.update { state ->
                    state.copy(folderPairName = action.name)
                }
            }
        }
    }
}