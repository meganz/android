package mega.privacy.android.app.presentation.bottomsheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.CreateShareKey
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.bottomsheet.model.NodeBottomSheetUIState
import mega.privacy.android.app.presentation.bottomsheet.model.NodeShareInformation
import mega.privacy.android.domain.usecase.MonitorConnectivity
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * View model associated with [NodeOptionsBottomSheetDialogFragment]
 */
@HiltViewModel
class NodeOptionsViewModel @Inject constructor(
    private val createShareKey: CreateShareKey,
    private val getNodeByHandle: GetNodeByHandle,
    private val monitorConnectivity: MonitorConnectivity,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val shareKeyCreated = MutableStateFlow<Boolean?>(null)

    private val _state = MutableStateFlow(NodeBottomSheetUIState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                savedStateHandle.getStateFlow(NODE_ID_KEY, -1L)
                    .map { getNodeByHandle(it) },
                savedStateHandle.getStateFlow(SHARE_DATA_KEY, null),
                shareKeyCreated,
                monitorConnectivity()
            ) { node: MegaNode?, shareData: NodeShareInformation?, shareKeyCreated: Boolean?, isOnline: Boolean ->
                { state: NodeBottomSheetUIState ->
                    state.copy(
                        node = node,
                        shareData = shareData,
                        shareKeyCreated = shareKeyCreated,
                        isOnline = isOnline,
                    )
                }
            }.collect {
                _state.update(it)
            }
        }
    }

    fun createShareKey() {
        viewModelScope.launch {
            kotlin.runCatching {
                val node = state.value.node
                    ?: throw IllegalArgumentException("Cannot create a share key for a null node")
                createShareKey(node)
            }.onSuccess {
                shareKeyCreated.emit(true)
            }.onFailure {
                shareKeyCreated.emit(false)
            }
        }
    }

    /**
     * Change the value of shareKeyCreated to false after it is consumed.
     */
    fun shareDialogDisplayed() {
        shareKeyCreated.tryEmit(null)
    }


    companion object {
        const val MODE_KEY = "MODE"
        const val NODE_ID_KEY = "NODE_ID_KEY"
        const val SHARE_DATA_KEY = "SHARE_DATA_KEY"
    }
}