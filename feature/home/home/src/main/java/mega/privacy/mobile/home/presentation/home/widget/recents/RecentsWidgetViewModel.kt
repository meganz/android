package mega.privacy.mobile.home.presentation.home.widget.recents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.SetHideRecentActivityUseCase
import mega.privacy.mobile.home.presentation.home.widget.recents.mapper.RecentActionUiItemMapper
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentsWidgetUiState
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RecentsWidgetViewModel @Inject constructor(
    private val getRecentActionsUseCase: GetRecentActionsUseCase,
    private val recentActionUiItemMapper: RecentActionUiItemMapper,
    private val setHideRecentActivityUseCase: SetHideRecentActivityUseCase,
    private val monitorHideRecentActivityUseCase: MonitorHideRecentActivityUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecentsWidgetUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadRecentActions()
        monitorHideRecentActivity()
    }

    /**
     * Load recent actions
     */
    private fun loadRecentActions() {
        viewModelScope.launch {
            runCatching {
                getRecentActionsUseCase(excludeSensitives = false) // TODO: Handle hidden nodes
                    .map { recentActionUiItemMapper(it) }
            }.onSuccess { buckets ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recentActionItems = buckets,
                    )
                }
            }.onFailure { throwable ->
                Timber.e(throwable, "Failed to load recent actions")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Monitor hide recent activity state
     */
    private fun monitorHideRecentActivity() {
        viewModelScope.launch {
            monitorHideRecentActivityUseCase()
                .catch {
                    Timber.e(it, "Failed to monitor hide recent activity")
                }
                .collectLatest { isHidden ->
                    _uiState.update { it.copy(isHideRecentsEnabled = isHidden) }
                }
        }
    }

    /**
     * Hide recent activity
     */
    fun hideRecentActivity() {
        viewModelScope.launch {
            runCatching {
                setHideRecentActivityUseCase(true)
            }.onFailure {
                Timber.e(it, "Failed to hide recent activity")
            }
        }
    }

    /**
     * Show recent activity
     */
    fun showRecentActivity() {
        viewModelScope.launch {
            runCatching {
                setHideRecentActivityUseCase(false)
            }.onFailure {
                Timber.e(it, "Failed to show recent activity")
            }
        }
    }
}