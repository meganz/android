package mega.privacy.android.app.presentation.node.dialogs.removesharefolder

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.dialog.shares.RemoveShareResultMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.ApplicationScope
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
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val getOutShareByNodeIdUseCase: GetOutShareByNodeIdUseCase,
    private val removeShareUseCase: RemoveShareUseCase,
    private val removeShareResultMapper: RemoveShareResultMapper,
    private val snackBarHandler: SnackBarHandler
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
        applicationScope.launch {
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
        applicationScope.launch {
            runCatching {
                removeShareUseCase(nodeIds)
            }.onFailure {
                Timber.e(it)
            }.onSuccess { result ->
                val message = removeShareResultMapper(result)
                snackBarHandler.postSnackbarMessage(message)
            }
        }
    }
}