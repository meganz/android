package mega.privacy.android.app.presentation.offline.offlinefileinfocompose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model.OfflineFileInfoUiState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFileInformationByIdUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import timber.log.Timber

/**
 * View Model class for [mega.privacy.android.app.presentation.offline.offlinefileinfocompose.view.OfflineFileInfoScreen]
 */
@HiltViewModel(assistedFactory = OfflineFileInfoComposeViewModel.Factory::class)
internal class OfflineFileInfoComposeViewModel @AssistedInject constructor(
    @Assisted nodeHandle: Long,
    private val getOfflineFileInformationByIdUseCase: GetOfflineFileInformationByIdUseCase,
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val nodeId = NodeId(nodeHandle)

    /**
     * private mutable UI state
     */
    private val _uiState = MutableStateFlow(OfflineFileInfoUiState())

    /**
     * public immutable UI State for view
     */
    val uiState = _uiState.asStateFlow()

    init {
        loadOfflineNodeInformation()
        monitorThemeMode()
    }

    private fun loadOfflineNodeInformation() {
        viewModelScope.launch {
            runCatching {
                getOfflineFileInformationByIdUseCase(nodeId, true)?.let { offlineFileInformation ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            offlineFileInformation = offlineFileInformation,
                        )
                    }
                } ?: run {
                    handleError()
                }
            }.onFailure {
                handleError()
                Timber.e(it)
            }
        }
    }

    private fun monitorThemeMode() {
        viewModelScope.launch {
            monitorThemeModeUseCase()
                .catch { Timber.e(it) }
                .collect { themeMode ->
                    _uiState.update { it.copy(themeMode = themeMode) }
                }
        }
    }

    private fun handleError() {
        _uiState.update {
            it.copy(
                errorEvent = triggered(true)
            )
        }
    }

    /**
     * Remove the node from database and cache storage.
     *
     * Runs in the application scope because the screen closes right after asking for it, which
     * clears this ViewModel and would cancel the removal half way.
     */
    fun removeFromOffline() {
        applicationScope.launch {
            runCatching {
                removeOfflineNodeUseCase(nodeId)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    fun onErrorEventConsumed() =
        _uiState.update { state -> state.copy(errorEvent = consumed()) }

    /**
     * Factory for [OfflineFileInfoComposeViewModel]
     */
    @AssistedFactory
    interface Factory {
        fun create(nodeHandle: Long): OfflineFileInfoComposeViewModel
    }
}
