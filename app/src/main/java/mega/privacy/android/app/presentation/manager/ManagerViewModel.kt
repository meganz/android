package mega.privacy.android.app.presentation.manager

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.domain.repository.GlobalUpdatesRepository
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to {@link mega.privacy.android.app.main.ManagerActivity}
 *
 * @param monitorNodeUpdates Monitor global node updates
 * @param globalUpdatesRepository Monitor global updates
 */
@HiltViewModel
class ManagerViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
    globalUpdatesRepository: GlobalUpdatesRepository,
) : ViewModel() {

    /**
     * Monitor user updates and dispatch to observers
     */
    val updateUsers: LiveData<List<MegaUser>> =
        globalUpdatesRepository.monitorGlobalUpdates()
            .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
            .also { Timber.d("onUsersUpdate") }
            .map { it.users?.toList() }
            .filterNotNull()
            .filterNot { it.isEmpty() }
            .asLiveData()

    /**
     * Monitor user alerts updates and dispatch to observers
     */
    val updateUserAlerts: LiveData<List<MegaUserAlert>> =
        globalUpdatesRepository.monitorGlobalUpdates()
            .filterIsInstance<GlobalUpdate.OnUserAlertsUpdate>()
            .also { Timber.d("onUserAlertsUpdate") }
            .map { it.userAlerts?.toList() }
            .filterNotNull()
            .filterNot { it.isEmpty() }
            .asLiveData()

    /**
     * Monitor global node updates and dispatch to observers
     */
    val updateNodes: LiveData<List<MegaNode>> =
        monitorNodeUpdates()
            .also { Timber.d("onNodesUpdate") }
            .filterNot { it.isEmpty() }
            .asLiveData()

    /**
     * Monitor contact request updates and dispatch to observers
     */
    val updateContactsRequests: LiveData<List<MegaContactRequest>> =
        globalUpdatesRepository.monitorGlobalUpdates()
            .filterIsInstance<GlobalUpdate.OnContactRequestsUpdate>()
            .also { Timber.d("onContactRequestsUpdate") }
            .map { it.requests?.toList() }
            .filterNotNull()
            .filterNot { it.isEmpty() }
            .asLiveData()

}