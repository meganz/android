package mega.privacy.android.app.presentation.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] class associated to InboxFragment
 *
 * @param monitorNodeUpdates Monitor global node updates
 * @param getCloudSortOrder Get the Cloud Sort Order
 */
@HiltViewModel
class InboxViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
    private val getCloudSortOrder: GetCloudSortOrder,
) : ViewModel() {

    /**
     * Monitor global node updates
     */
    val updateNodes =
        monitorNodeUpdates()
            .also { Timber.d("onNodesUpdate()") }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
            .map { Event(it) }
            .asLiveData()

    /**
     * Get Cloud Sort Order
     */
    fun getOrder() = runBlocking { getCloudSortOrder() }
}