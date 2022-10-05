package mega.privacy.android.app.presentation.recentactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetRecentActions
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import nz.mega.sdk.MegaRecentActionBucket
import javax.inject.Inject

/**
 * ViewModel associated to [RecentActionsFragment]
 *
 * @param getRecentActions
 * @param monitorNodeUpdates
 */
@HiltViewModel
class RecentActionsViewModel @Inject constructor(
    getRecentActions: GetRecentActions,
    monitorNodeUpdates: MonitorNodeUpdates,
) : ViewModel() {

    private val _buckets = MutableStateFlow<List<MegaRecentActionBucket>>(emptyList())

    /**
     * Current recent actions bucket list
     */
    val buckets = _buckets.asStateFlow()

    /**
     * Selected recent actions bucket
     */
    var selected: MegaRecentActionBucket? = null

    /**
     * Snapshot of recent actions bucket list
     */
    var snapShotActionList: List<MegaRecentActionBucket>? = null

    init {
        viewModelScope.launch {
            _buckets.emit(getRecentActions())
            monitorNodeUpdates().collectLatest {
                _buckets.emit(getRecentActions())
            }
        }
    }

    /**
     * Set the selected recent actions bucket and current recent actions bucket list
     */
    fun select(bucket: MegaRecentActionBucket) {
        selected = bucket
        snapShotActionList = buckets.value
    }
}