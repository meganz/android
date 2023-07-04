package mega.privacy.android.feature.sync.ui.megapicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetTypedNodesFromFolderUseCase
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.usecase.SetSelectedMegaFolderUseCase
import javax.inject.Inject

@HiltViewModel
internal class MegaPickerViewModel @Inject constructor(
    private val setSelectedMegaFolderUseCase: SetSelectedMegaFolderUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getTypedNodesFromFolder: GetTypedNodesFromFolderUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MegaPickerState())
    val state: StateFlow<MegaPickerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val rootFolder = getRootNodeUseCase()
            rootFolder?.let(::fetchFolders)
        }
    }

    fun handleAction(action: MegaPickerAction) {
        when (action) {
            is MegaPickerAction.FolderClicked -> {
                fetchFolders(action.folder)
            }

            MegaPickerAction.CurrentFolderSelected -> {
                val id = state.value.currentFolder?.id?.longValue
                val name = state.value.currentFolder?.name
                if (id != null && name != null) {
                    setSelectedMegaFolderUseCase(RemoteFolder(id, name))
                }
            }
        }
    }

    private fun fetchFolders(currentFolder: Node) {
        viewModelScope.launch {
            getTypedNodesFromFolder(currentFolder.id).collectLatest { childFolders ->
                _state.update {
                    it.copy(
                        currentFolder = currentFolder, nodes = childFolders
                    )
                }
            }
        }
    }
}