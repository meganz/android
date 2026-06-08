package mega.privacy.android.feature.cloudexplorer.presentation.addvideotoplaylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddVideoToPlaylistViewModel @Inject constructor(
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
) : ViewModel() {

    val uiState: StateFlow<AddVideoToPlaylistUiState> by lazy(LazyThreadSafetyMode.NONE) {
        flow {
            emit(
                runCatching { getRootNodeIdUseCase() }
                    .onFailure { Timber.e(it) }
                    .getOrNull() ?: NodeId(-1)
            )
        }.map { rootNodeId ->
            AddVideoToPlaylistUiState.Data(
                rootNodeId = rootNodeId,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, AddVideoToPlaylistUiState.Loading)
    }
}
