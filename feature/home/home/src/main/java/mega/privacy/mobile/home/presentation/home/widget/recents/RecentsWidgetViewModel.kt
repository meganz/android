package mega.privacy.mobile.home.presentation.home.widget.recents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionsUseCase
import mega.privacy.mobile.home.presentation.home.widget.recents.mapper.RecentActionUiItemMapper
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentsWidgetUiState
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RecentsWidgetViewModel @Inject constructor(
    private val getRecentActionsUseCase: GetRecentActionsUseCase,
    private val recentActionUiItemMapper: RecentActionUiItemMapper,
) : ViewModel() {

    /** private mutable UI state */
    private val _uiState = MutableStateFlow(RecentsWidgetUiState())

    /** public UI state */
    val uiState = _uiState.asStateFlow()

    init {
        loadRecentActions()
    }

    /**
     * Load recent actions
     */
    private fun loadRecentActions() {
        viewModelScope.launch {
            runCatching {
                getRecentActionsUseCase(excludeSensitives = false)
            }.onSuccess { buckets ->
                val recentActionItems = buckets
                    .map { recentActionUiItemMapper(it) }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recentActionItems = recentActionItems,
                    )
                }
            }.onFailure {
                Timber.e(it, "Failed to load recent actions")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}