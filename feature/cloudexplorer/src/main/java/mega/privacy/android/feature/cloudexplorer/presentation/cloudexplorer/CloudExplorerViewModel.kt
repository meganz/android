package mega.privacy.android.feature.cloudexplorer.presentation.cloudexplorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import javax.inject.Inject

@HiltViewModel
class CloudExplorerViewModel @Inject constructor(
    private val getRootNodeUseCase: GetRootNodeUseCase,
) : ViewModel() {
    private val _uiStateFlow = MutableStateFlow(CloudExplorerUiState())
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            _uiStateFlow.update { state ->
                state.copy(
                    currentFolder = getRootNodeUseCase() as? FolderNode
                )
            }
        }
    }
}