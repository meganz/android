package mega.privacy.mobile.home.presentation.recents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.recentactions.ClearRecentActivityUseCase
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionsUseCase
import mega.privacy.android.domain.usecase.recentactions.MonitorRecentActivityClearedUseCase
import mega.privacy.android.domain.usecase.setting.MonitorHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.SetHideRecentActivityUseCase
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.recents.mapper.RecentActionUiItemMapper
import mega.privacy.mobile.home.presentation.recents.model.RecentsUiState
import timber.log.Timber
import java.time.Instant

@HiltViewModel(assistedFactory = RecentsViewModel.Factory::class)
class RecentsViewModel @AssistedInject constructor(
    @Assisted private val maxBucketCount: Int,
    private val getRecentActionsUseCase: GetRecentActionsUseCase,
    private val recentActionUiItemMapper: RecentActionUiItemMapper,
    private val setHideRecentActivityUseCase: SetHideRecentActivityUseCase,
    private val clearRecentActivityUseCase: ClearRecentActivityUseCase,
    private val monitorHideRecentActivityUseCase: MonitorHideRecentActivityUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    private val monitorRecentActivityClearedUseCase: MonitorRecentActivityClearedUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecentsUiState())
    val uiState = _uiState.asStateFlow()
    private var loadRecentsJob: Job? = null

    init {
        loadRecents()
        monitorHiddenNodesState()
        monitorHideRecentActivity()
        monitorNodeUpdates()
        monitorFetchNodesFinish()
        monitorRecentActivityCleared()
    }

    private fun monitorRecentActivityCleared() {
        viewModelScope.launch {
            monitorRecentActivityClearedUseCase()
                .catch { Timber.e(it) }
                .collect { loadRecents() }
        }
    }

    private fun monitorFetchNodesFinish() {
        viewModelScope.launch {
            monitorFetchNodesFinishUseCase()
                .catch { Timber.e(it) }
                .collect { finished ->
                    if (finished) {
                        loadRecents()
                    }
                }
        }
    }

    private fun loadRecents() {
        loadRecentsJob?.cancel()
        loadRecentsJob = viewModelScope.launch {
            runCatching {
                getRecentActionsUseCase(
                    excludeSensitives = uiState.value.excludeSensitives,
                    maxBucketCount = maxBucketCount
                ).map { recentActionUiItemMapper(it) }.distinctBy { it.key }
            }.onSuccess { buckets ->
                // Only update state if this coroutine not cancelled
                if (isActive) {
                    _uiState.update {
                        it.copy(
                            isNodesLoading = false,
                            recentActionItems = buckets,
                        )
                    }
                }
            }.onFailure { throwable ->
                Timber.e(throwable, "Failed to load recent actions")
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun monitorNodeUpdates() {
        viewModelScope.launch {
            // Wait till the initial loading is done
            uiState
                .map { it.isLoading }
                .first { !it }

            monitorNodeUpdatesUseCase()
                .catch { Timber.e(it) }
                .conflate()
                .debounce(500L)
                .collect {
                    loadRecents()
                }
        }
    }

    private fun monitorHiddenNodesState() {
        viewModelScope.launch {
            combine(
                monitorHiddenNodesEnabledUseCase(),
                monitorShowHiddenItemsUseCase()
            ) { isHiddenNodesEnabled, showHiddenNodes ->
                isHiddenNodesEnabled to showHiddenNodes
            }
                .catch {
                    Timber.e(it, "Failed to monitor hidden nodes state")
                }
                .collectLatest { (isHiddenNodesEnabled, showHiddenNodes) ->
                    val newExcludeSensitives = isHiddenNodesEnabled && !showHiddenNodes
                    val isReloadRequired = uiState.value.excludeSensitives != newExcludeSensitives
                    _uiState.update {
                        it.copy(
                            isHiddenNodeSettingsLoading = false,
                            isNodesLoading = isReloadRequired || it.isNodesLoading, // keep showing loading state while reloading node buckets
                            isHiddenNodesEnabled = isHiddenNodesEnabled,
                            showHiddenNodes = showHiddenNodes,
                        )
                    }
                    if (isReloadRequired) {
                        loadRecents()
                    }
                }
        }
    }

    private fun monitorHideRecentActivity() {
        viewModelScope.launch {
            monitorHideRecentActivityUseCase()
                .catch {
                    Timber.e(it, "Failed to monitor hide recent activity")
                }
                .collectLatest { enabled ->
                    _uiState.update { it.copy(isHideRecentsEnabled = enabled) }
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

    /**
     * Clear recent activity
     */
    fun clearRecentActivity() {
        viewModelScope.launch {
            runCatching {
                clearRecentActivityUseCase(Instant.now().epochSecond)
            }.onSuccess {
                _uiState.update {
                    it.copy(recentsClearedEvent = triggered)
                }
                snackbarEventQueue.queueMessage(sharedR.string.home_recents_snackbar_activity_cleared)
            }.onFailure {
                Timber.e(it, "Failed to clear recent activity")
            }
        }
    }

    /**
     * Consume recent activity cleared event
     */
    fun onRecentsClearedEventConsumed() {
        _uiState.update {
            it.copy(recentsClearedEvent = consumed)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(maxBucketCount: Int): RecentsViewModel
    }
}

object RecentsWidgetConstants {
    /**
     * Maximum number of recent action buckets to display in the widget
     */
    const val WIDGET_MAX_BUCKETS = 4

    /**
     * Maximum number of recent action buckets to display in the full screen
     */
    const val SCREEN_MAX_BUCKETS = 500
}