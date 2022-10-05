package mega.privacy.android.app.presentation.recentactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetRecentActions
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.recentactions.model.RecentActionItemType
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
     * List of recent actions to display
     */
    val recentActionsItems =
        _buckets
            .map { reloadItems(it) }
            .stateIn(viewModelScope,
                SharingStarted.WhileSubscribed(),
                emptyList()
            )

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
        snapShotActionList = _buckets.value
    }

    /**
     * Format a list of [RecentActionItemType] from a [MegaRecentActionBucket]
     *
     * @param buckets
     * @return a list of [RecentActionItemType]
     */
    private fun reloadItems(buckets: List<MegaRecentActionBucket>): List<RecentActionItemType> {
        val recentItemList = arrayListOf<RecentActionItemType>()
        var previousDate: Long? = null
        var currentDate: Long
        for (i in buckets.indices) {
            val item =
                RecentActionItemType.Item(buckets[i])
            if (i == 0) {
                currentDate = item.timestamp
                previousDate = currentDate
                recentItemList.add(RecentActionItemType.Header(currentDate))
            } else {
                currentDate = item.timestamp
                if (currentDate != previousDate) {
                    recentItemList.add(RecentActionItemType.Header(currentDate))
                    previousDate = currentDate
                }
            }
            recentItemList.add(item)
        }
        return recentItemList
    }
}