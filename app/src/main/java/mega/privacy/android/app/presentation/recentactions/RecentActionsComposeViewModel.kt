package mega.privacy.android.app.presentation.recentactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.recentactions.mapper.RecentActionBucketUiEntityMapper
import mega.privacy.android.app.presentation.recentactions.model.RecentActionsUiState
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.SetHideRecentActivityUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to [RecentActionsComposeFragment]
 */
@HiltViewModel
class RecentActionsComposeViewModel @Inject constructor(
    private val getRecentActionsUseCase: GetRecentActionsUseCase,
    private val setHideRecentActivityUseCase: SetHideRecentActivityUseCase,
    private val recentActionBucketUiEntityMapper: RecentActionBucketUiEntityMapper,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    monitorHideRecentActivityUseCase: MonitorHideRecentActivityUseCase,
    monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
) : ViewModel() {

    /** private mutable UI state */
    private val _uiState = MutableStateFlow(RecentActionsUiState())

    /** public UI state */
    val uiState = _uiState.asStateFlow()

    /**
     * Selected recent actions bucket
     */
    var selectedBucket: RecentActionBucket? = null

    init {
        viewModelScope.launch {
            val showHiddenItems = _uiState.value.showHiddenItems
            updateRecentActions(
                excludeSensitives = !showHiddenItems,
            )
        }

        viewModelScope.launch {
            monitorNodeUpdatesUseCase()
                .catch {
                    Timber.e(it)
                }
                .conflate()
                .collect {
                    val showHiddenItems = _uiState.value.showHiddenItems
                    updateRecentActions(
                        excludeSensitives = !showHiddenItems,
                    )
                }
        }

        viewModelScope.launch {
            monitorHideRecentActivityUseCase()
                .catch {
                    Timber.e(it)
                }
                .collectLatest {
                    setUiHideRecentActivity(it)
                }
        }

        viewModelScope.launch {
            monitorConnectivityUseCase().collect {
                _uiState.update { state -> state.copy(isConnected = it) }
            }
        }

        viewModelScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) {
                monitorAccountDetailUseCase()
                    .collectLatest {
                        _uiState.update { state ->
                            state.copy(accountType = it.levelDetail?.accountType)
                        }
                    }
            }
        }

        viewModelScope.launch {
            monitorShowHiddenItemsUseCase()
                .collectLatest { showHiddenItems ->
                    _uiState.update { state ->
                        state.copy(showHiddenItems = showHiddenItems)
                    }
                    updateRecentActions(
                        excludeSensitives = !showHiddenItems,
                    )
                }
        }
    }

    /**
     * Update the recent actions list
     */
    private suspend fun updateRecentActions(
        excludeSensitives: Boolean,
    ) {
        runCatching {
            getRecentActionsUseCase(
                excludeSensitives = excludeSensitives,
            )
        }.onSuccess { list ->
            val groupedRecentActions = list
                .map { recentActionBucketUiEntityMapper(it) }
                .groupBy { it.date }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    groupedRecentActionItems = groupedRecentActions,
                )
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Set the selected recent actions bucket
     *
     * @param bucket
     */
    fun selectBucket(bucket: RecentActionBucket) {
        selectedBucket = bucket
    }

    /**
     * Disable hide recent actions activity setting
     */
    fun disableHideRecentActivitySetting() = viewModelScope.launch {
        setHideRecentActivityUseCase(false)
    }

    /**
     * Set hide recent activity ui state
     */
    private fun setUiHideRecentActivity(hide: Boolean) {
        _uiState.update { it.copy(hideRecentActivity = hide) }
    }

    /**
     * Get all recent action buckets
     */
    fun getAllRecentBuckets() = uiState.value
        .groupedRecentActionItems
        .values
        .flatten()
        .map { it.bucket }
}
