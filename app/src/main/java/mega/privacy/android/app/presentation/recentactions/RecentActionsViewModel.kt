package mega.privacy.android.app.presentation.recentactions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.domain.usecase.GetRecentActions
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.recentactions.model.RecentActionItemType
import mega.privacy.android.app.utils.SharedPreferenceConstants
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.GetVisibleContacts
import nz.mega.sdk.MegaRecentActionBucket
import javax.inject.Inject

/**
 * ViewModel associated to [RecentActionsFragment]
 *
 * @param getRecentActions
 * @param getVisibleContacts
 * @param monitorNodeUpdates
 */
@HiltViewModel
class RecentActionsViewModel @Inject constructor(
    private val getRecentActions: GetRecentActions,
    private val getVisibleContacts: GetVisibleContacts,
    @ApplicationContext private val applicationContext: Context,
    monitorNodeUpdates: MonitorNodeUpdates,
) : ViewModel() {

    private var _buckets = listOf<MegaRecentActionBucket>()

    private val _recentActionsItems = MutableStateFlow<List<RecentActionItemType>>(emptyList())

    /**
     * List of recent actions to display
     */
    val recentActionsItems = _recentActionsItems.asStateFlow()

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
            monitorNodeUpdates().collectLatest {
                updateRecentActions()
            }
        }
        updateRecentActions()
    }

    /**
     * Set the selected recent actions bucket and current recent actions bucket list
     */
    fun select(bucket: MegaRecentActionBucket) {
        selected = bucket
        snapShotActionList = _buckets
    }

    /**
     * Disable the hide recent actions activity settings
     */
    fun disableHideRecentActionsActivitySettings() {
        LiveEventBus.get(EventConstants.EVENT_UPDATE_HIDE_RECENT_ACTIVITY, Boolean::class.java)
            .post(false)
        applicationContext.getSharedPreferences(SharedPreferenceConstants.USER_INTERFACE_PREFERENCES,
            Context.MODE_PRIVATE)
            .edit().putBoolean(SharedPreferenceConstants.HIDE_RECENT_ACTIVITY, false).apply()
    }

    /**
     * Retrieve the hide recent actions activity settings
     */
    fun getHideRecentActionsActivitySettings(): Boolean {
        return applicationContext
            .getSharedPreferences(SharedPreferenceConstants.USER_INTERFACE_PREFERENCES,
                Context.MODE_PRIVATE)
            .getBoolean(SharedPreferenceConstants.HIDE_RECENT_ACTIVITY, false)
    }

    /**
     * Update the recent actions list
     */
    private fun updateRecentActions() = viewModelScope.launch {
        val getRecentActions = async {
            getRecentActions().also { _buckets = it }
        }
        val getVisibleContacts = async { getVisibleContacts() }

        val formattedList =
            formatRecentActions(getRecentActions.await(), getVisibleContacts.await())
        _recentActionsItems.emit(formattedList)
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