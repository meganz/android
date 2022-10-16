package mega.privacy.android.app.presentation.recentactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetRecentActions
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.recentactions.model.RecentActionItemType
import mega.privacy.android.app.presentation.recentactions.model.RecentActionsState
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.GetVisibleContacts
import mega.privacy.android.domain.usecase.MonitorHideRecentActivity
import mega.privacy.android.domain.usecase.SetHideRecentActivity
import nz.mega.sdk.MegaRecentActionBucket
import javax.inject.Inject

/**
 * ViewModel associated to [RecentActionsFragment]
 *
 * @param getRecentActions
 * @param getVisibleContacts
 * @param getVisibleContacts
 * @param setHideRecentActivity
 * @param monitorNodeUpdates
 */
@HiltViewModel
class RecentActionsViewModel @Inject constructor(
    private val getRecentActions: GetRecentActions,
    private val getVisibleContacts: GetVisibleContacts,
    private val setHideRecentActivity: SetHideRecentActivity,
    monitorHideRecentActivity: MonitorHideRecentActivity,
    monitorNodeUpdates: MonitorNodeUpdates,
) : ViewModel() {

    private var _buckets = listOf<MegaRecentActionBucket>()

    /** private UI state */
    private val _state = MutableStateFlow(RecentActionsState())

    /** public UI state */
    val state: StateFlow<RecentActionsState> = _state

    /**
     * Selected recent actions bucket
     */
    var selected: MegaRecentActionBucket? = null

    /**
     * Snapshot of recent actions bucket list when a user select one item
     */
    var snapshotActionList: List<MegaRecentActionBucket>? = null

    init {
        updateRecentActions()

        // monitor node updates
        viewModelScope.launch {
            monitorNodeUpdates().collectLatest {
                updateRecentActions()
            }
        }
        // monitor hide recent activity preference
        viewModelScope.launch {
            monitorHideRecentActivity().collectLatest {
                setUiHideRecentActivity(it)
            }
        }
    }

    /**
     * Set the selected recent actions bucket and current recent actions bucket list
     *
     * @param bucket
     */
    fun select(bucket: MegaRecentActionBucket) {
        selected = bucket
        snapshotActionList = _buckets
    }

    /**
     * Disable hide recent actions activity setting
     */
    fun disableHideRecentActivitySetting() = viewModelScope.launch {
        setHideRecentActivity(false)
    }

    /**
     * Set recent actions items ui state
     *
     * @param list
     */
    private fun setUiRecentActionsItems(list: List<RecentActionItemType>) {
        _state.update { it.copy(recentActionItems = list) }
    }

    /**
     * Set hide recent activity ui state
     */
    private fun setUiHideRecentActivity(hide: Boolean) {
        _state.update { it.copy(hideRecentActivity = hide) }
    }

    /**
     * Update the recent actions list by combination
     */
    private fun updateRecentActions() = viewModelScope.launch {
        val getRecentActions = async {
            getRecentActions().also { _buckets = it }
        }
        val getVisibleContacts = async { getVisibleContacts() }

        val formattedList =
            formatRecentActions(getRecentActions.await(), getVisibleContacts.await())

        setUiRecentActionsItems(formattedList)
    }

    /**
     * Format a list of [RecentActionItemType] from a [MegaRecentActionBucket]
     *
     * @param buckets
     * @return a list of [RecentActionItemType]
     */
    private fun formatRecentActions(
        buckets: List<MegaRecentActionBucket>,
        visibleContacts: List<ContactItem>,
    ): List<RecentActionItemType> {

        val recentItemList = arrayListOf<RecentActionItemType>()
        var previousDate: Long? = null

        buckets.forEach { bucket ->
            val currentDate = bucket.timestamp

            if (currentDate != previousDate) {
                previousDate = currentDate
                recentItemList.add(RecentActionItemType.Header(currentDate))
            }

            val userName =
                visibleContacts.find { bucket.userEmail == it.email }?.contactData?.fullName.orEmpty()
            recentItemList.add(RecentActionItemType.Item(bucket, userName))
        }

        return recentItemList
    }
}