package mega.privacy.android.app.presentation.node.dialogs.removesharefolder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.dialog.shares.RemoveShareResultMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.RemoveShareUseCase
import mega.privacy.android.domain.usecase.shares.GetOutShareByNodeIdUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for removeShare folder
 * @property getOutShareByNodeIdUseCase [GetOutShareByNodeIdUseCase]
 * @property removeShareUseCase [RemoveShareUseCase]
 * @property removeShareResultMapper [RemoveShareResultMapper]
 */
@HiltViewModel
class RemoveShareFolderViewModel @Inject constructor(
    private val getOutShareByNodeIdUseCase: GetOutShareByNodeIdUseCase,
    private val removeShareUseCase: RemoveShareUseCase,
    private val removeShareResultMapper: RemoveShareResultMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(RemoveShareFolderState())

    /**
     * State
     */
    val state = _state.asStateFlow()

    /**
     * Get Contact info for shared folder with folders are shared
     * @param nodeIds
     */
    fun getContactInfoForSharedFolder(nodeIds: List<NodeId>) {
        viewModelScope.launch {
            if (nodeIds.size == 1) {
                runCatching {
                    getOutShareByNodeIdUseCase(nodeIds[0])
                }.onSuccess { shares ->
                    _state.update {
                        it.copy(
                            numberOfShareContact = shares.size,
                            numberOfShareFolder = 1
                        )
                    }
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }

    /**
     * Remove Folder share
     * @param nodeIds list of node handles
     */
    fun removeShare(nodeIds: List<NodeId>) {
        viewModelScope.launch {
            runCatching {
                removeShareUseCase(nodeIds)
            }.onFailure {
                Timber.e(it)
            }.onSuccess { result ->
                val message = removeShareResultMapper(result)
                _state.update { state ->
                    state.copy(
                        removeFolderShareEvent = triggered(message),
                    )
                }
            }
        }
    }

    /**
     * Consumed remove folder event
     */
    fun consumeRemoveShareEvent() {
        _state.update {
            it.copy(removeFolderShareEvent = consumed())
        }
    }
}