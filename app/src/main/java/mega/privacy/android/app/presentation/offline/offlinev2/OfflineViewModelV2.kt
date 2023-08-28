package mega.privacy.android.app.presentation.offline.offlinev2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.offline.offlinev2.model.OfflineUIState
import mega.privacy.android.domain.usecase.LoadOfflineNodesUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * OfflineViewModelV2 of [OfflineFragmentV2]
 */
@HiltViewModel
class OfflineViewModelV2 @Inject constructor(
    private val loadOfflineNodesUseCase: LoadOfflineNodesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineUIState())

    /**
     * Flow of [OfflineUIState] UI State
     */
    val uiState = _uiState.asStateFlow()

    init {
        loadOfflineNodes()
    }

    /**
     * get the offline nodes
     * @param path the provided path to fetch the offline nodes from
     * @param searchQuery the provided search query to load the offline nodes based on from database
     */
    fun loadOfflineNodes(path: String = "", searchQuery: String = "") {
        _uiState.update { state ->
            state.copy(isLoading = true)
        }
        viewModelScope.launch {
            runCatching {
                loadOfflineNodesUseCase(path = path, searchQuery = searchQuery)
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(offlineNodes = it, isLoading = false)
                }
            }.onFailure {
                Timber.e(it, "Exception fetching offline nodes")
                _uiState.update { state ->
                    state.copy(offlineNodes = null, isLoading = false)
                }
            }
        }
    }
}