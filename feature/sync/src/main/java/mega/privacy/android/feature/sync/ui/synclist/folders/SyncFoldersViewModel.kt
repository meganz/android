package mega.privacy.android.feature.sync.ui.synclist.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.GetSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.PauseSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.RemoveFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.ResumeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.SetUserPausedSyncUseCase
import mega.privacy.android.feature.sync.ui.mapper.SyncUiItemMapper
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.RemoveFolderClicked
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.PauseRunClicked
import javax.inject.Inject

@HiltViewModel
internal class SyncFoldersViewModel @Inject constructor(
    private val syncUiItemMapper: SyncUiItemMapper,
    private val removeFolderPairUseCase: RemoveFolderPairUseCase,
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
    private val resumeSyncUseCase: ResumeSyncUseCase,
    private val pauseSyncUseCase: PauseSyncUseCase,
    private val getSyncStalledIssuesUseCase: GetSyncStalledIssuesUseCase,
    private val setUserPausedSyncsUseCase: SetUserPausedSyncUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncFoldersState(emptyList()))
    val state: StateFlow<SyncFoldersState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorSyncsUseCase()
                .map(syncUiItemMapper::invoke)
                .map { syncs ->
                    val stalledIssues = getSyncStalledIssuesUseCase()
                    syncs.map { sync ->
                        sync.copy(hasStalledIssues = stalledIssues.any {
                            it.localPaths.firstOrNull()?.contains(sync.deviceStoragePath)
                                ?: (it.nodeNames.first().contains(sync.megaStoragePath))
                        })
                    }
                }
                .collectLatest { syncs ->
                    _state.update {
                        SyncFoldersState(syncs)
                    }
                }
        }
    }

    fun handleAction(action: SyncFoldersAction) {
        when (action) {
            is SyncFoldersAction.CardExpanded -> {
                val syncUiItem = action.syncUiItem
                val expanded = action.expanded

                _state.update { state ->
                    state.copy(
                        syncUiItems = _state.value.syncUiItems.map {
                            if (it.id == syncUiItem.id) {
                                it.copy(expanded = expanded)
                            } else {
                                it
                            }
                        }
                    )
                }
            }

            is RemoveFolderClicked -> {
                viewModelScope.launch {
                    removeFolderPairUseCase(action.folderPairId)
                }
            }

            is PauseRunClicked -> {
                viewModelScope.launch {
                    if (action.syncUiItem.status != SyncStatus.PAUSED) {
                        pauseSyncUseCase(action.syncUiItem.id)
                        setUserPausedSyncsUseCase(action.syncUiItem.id, true)
                    } else {
                        resumeSyncUseCase(action.syncUiItem.id)
                        setUserPausedSyncsUseCase(action.syncUiItem.id, false)
                    }
                }
            }
        }
    }
}
