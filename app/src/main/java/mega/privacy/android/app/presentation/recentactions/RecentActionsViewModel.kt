package mega.privacy.android.app.presentation.recentactions

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import nz.mega.sdk.MegaRecentActionBucket
import javax.inject.Inject

/**
 * ViewModel associated to [RecentActionsFragment]
 */
@HiltViewModel
class RecentActionsViewModel @Inject constructor(
) : ViewModel() {

    /**
     * Selected recent actions bucket
     */
    var selected: MegaRecentActionBucket? = null

    /**
     * Current recent actions bucket list
     */
    var currentActionList: List<MegaRecentActionBucket>? = null

    /**
     * Set the selected recent actions bucket and current recent actions bucket list
     */
    fun select(bucket: MegaRecentActionBucket, currentActions: List<MegaRecentActionBucket>) {
        selected = bucket
        currentActionList = currentActions
    }
}