package mega.privacy.android.feature.cloudexplorer.presentation.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.shared.nodes.extension.orInvalid
import timber.log.Timber
import javax.inject.Inject

/**
 * Shared ViewModel for explorer flows that only need the cloud-drive root to open at.
 */
@HiltViewModel
internal class NodePickerViewModel @Inject constructor(
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
) : ViewModel() {

    val uiState: StateFlow<NodePickerUiState> by lazy(LazyThreadSafetyMode.NONE) {
        flow {
            emit(getRootNodeIdUseCase.orInvalid())
        }.map { rootNodeId ->
            NodePickerUiState.Data(rootNodeId = rootNodeId)
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, NodePickerUiState.Loading)
    }
}
