package mega.privacy.android.app.presentation.manager

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.domain.usecase.*
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to [mega.privacy.android.app.main.ManagerActivity]
 *
 * @param monitorNodeUpdates Monitor global node updates
 * @param monitorGlobalUpdates Monitor global updates
 * @param setRubbishParentHandle Set rubbish parent handle in memory
 * @param getManagerParentHandle Get rubbish parent handle previously set in memory
 */
@HiltViewModel
class ManagerViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
    monitorGlobalUpdates: MonitorGlobalUpdates,
    private val setManagerParentHandle: SetManagerParentHandle,
    private val getManagerParentHandle: GetManagerParentHandle
) : ViewModel() {

    /**
     * Accessors to the current rubbish bin parent handle set in memory
     */
    var rubbishBinParentHandle: Long
        get() = getManagerParentHandle(GetManagerParentHandleType.RubbishBin)
        set(value) = setManagerParentHandle(SetManagerParentHandleType.RubbishBin, value)

    /**
     * Accessors to the current browser parent handle set in memory
     */
    var browserParentHandle: Long
        get() = getManagerParentHandle(GetManagerParentHandleType.Browser)
        set(value) = setManagerParentHandle(SetManagerParentHandleType.Browser, value)

    /**
     * Monitor all global updates
     */
    @Suppress("DEPRECATION")
    private val updates = monitorGlobalUpdates()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Monitor user updates and dispatch to observers
     */
    val updateUsers: LiveData<List<MegaUser>> =
        updates
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
        updates
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
        updates
            .filterIsInstance<GlobalUpdate.OnContactRequestsUpdate>()
            .also { Timber.d("onContactRequestsUpdate") }
            .map { it.requests?.toList() }
            .filterNotNull()
            .filterNot { it.isEmpty() }
            .asLiveData()

}