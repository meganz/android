package mega.privacy.android.app.presentation.offline.offlinecompose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineUIState
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineNodeUIItem
import mega.privacy.android.domain.usecase.LoadOfflineNodesUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * OfflineViewModelV2 of [OfflineFragmentCompose]
 */
@HiltViewModel
class OfflineComposeViewModel @Inject constructor(
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
     * map a list of [OfflineNodeInformation] to [OfflineNodeUIItem] to be used in the presentation layer
     */
    private fun getOfflineNodeUiItems(nodeList: List<OfflineNodeInformation>): List<OfflineNodeUIItem<OfflineNodeInformation>> {
        val offlineNodeList = _uiState.value.offlineNodes
        return nodeList.mapIndexed { index, it ->
            val isSelected =
                _uiState.value.selectedNodeHandles.contains(it.handle)
            OfflineNodeUIItem(
                offlineNode = it,
                isSelected = if (index < offlineNodeList.size) isSelected else false,
                isInvisible = if (index > offlineNodeList.size) offlineNodeList[index].isInvisible else false
            )
        }
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
                val offlineNodes = getOfflineNodeUiItems(it)
                _uiState.update { state ->
                    state.copy(offlineNodes = offlineNodes, isLoading = false)
                }
            }.onFailure {
                Timber.e(it, "Exception fetching offline nodes")
                _uiState.update { state ->
                    state.copy(offlineNodes = emptyList(), isLoading = false)
                }
            }
        }
    }
}