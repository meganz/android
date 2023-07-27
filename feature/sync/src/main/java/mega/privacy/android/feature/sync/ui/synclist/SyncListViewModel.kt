package mega.privacy.android.feature.sync.ui.synclist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.RemoveFolderPairUseCase
import mega.privacy.android.feature.sync.ui.mapper.SyncUiItemMapper
import mega.privacy.android.feature.sync.ui.synclist.SyncListAction.CardExpanded
import javax.inject.Inject

@HiltViewModel
internal class SyncListViewModel @Inject constructor(
    private val syncUiItemMapper: SyncUiItemMapper,
    private val removeFolderPairUseCase: RemoveFolderPairUseCase,
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncListState(emptyList()))
    val state: StateFlow<SyncListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorSyncsUseCase()
                .map(syncUiItemMapper::invoke)
                .collectLatest { syncs ->
                    _state.value = SyncListState(syncs)
                }
        }
    }

    fun handleAction(action: SyncListAction) {
        when (action) {
            is CardExpanded -> {
                val syncUiItem = action.syncUiItem
                val expanded = action.expanded

                _state.value = _state.value.copy(
                    syncUiItems = _state.value.syncUiItems.map {
                        if (it.id == syncUiItem.id) {
                            it.copy(expanded = expanded)
                        } else {
                            it
                        }
                    }
                )
            }

            is SyncListAction.RemoveFolderClicked -> {
                viewModelScope.launch {
                    removeFolderPairUseCase(action.folderPairId)
                }
            }
        }
    }
}