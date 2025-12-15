package mega.privacy.android.app.listeners.global

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.notifications.NotifyNotificationCountChangeUseCase
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaGlobalListenerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import javax.inject.Inject

/**
 * Application's Global Listener
 */
class GlobalListener @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val notifyNotificationCountChangeUseCase: NotifyNotificationCountChangeUseCase,
    private val globalOnContactRequestsUpdateHandler: GlobalOnContactRequestsUpdateHandler,
    private val globalOnUserUpdateHandler: GlobalOnUserUpdateHandler,
    private val globalOnNodesUpdateHandler: GlobalOnNodesUpdateHandler,
    private val globalOnAccountUpdateHandler: GlobalOnAccountUpdateHandler,
    private val globalOnEventHandler: GlobalOnEventHandler,
    private val globalOnGlobalSyncStateChangedHandler: GlobalOnGlobalSyncStateChangedHandler,
) : MegaGlobalListenerInterface {
    /**
     * onUsersUpdate
     */
    override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser>?) {
        globalOnUserUpdateHandler(users, api)
    }

    /**
     * onUserAlertsUpdate
     */
    override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert>?) {
        applicationScope.launch {
            notifyNotificationCountChangeUseCase()
        }
    }

    /**
     * onNodesUpdate
     */
    override fun onNodesUpdate(api: MegaApiJava, nodeList: ArrayList<MegaNode>?) {
        globalOnNodesUpdateHandler(nodeList)
    }

    /**
     * onAccountUpdate
     */
    override fun onAccountUpdate(api: MegaApiJava) {
        globalOnAccountUpdateHandler()
    }

    /**
     * onContactRequestsUpdate
     */
    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest>?,
    ) {
        globalOnContactRequestsUpdateHandler(requests)
    }

    /**
     * onEvent
     */
    override fun onEvent(api: MegaApiJava, event: MegaEvent?) {
        globalOnEventHandler(event)
    }

    /**
     * onSetsUpdate
     */
    override fun onSetsUpdate(api: MegaApiJava, sets: ArrayList<MegaSet>?) {
        Timber.d("Sets Updated")
    }

    /**
     * onSetElementsUpdate
     */
    override fun onSetElementsUpdate(
        api: MegaApiJava,
        elements: ArrayList<MegaSetElement>?,
    ) {
        Timber.d("Set elements updated")
    }

    override fun onGlobalSyncStateChanged(api: MegaApiJava) {
        Timber.d("Global sync state changed")
        globalOnGlobalSyncStateChangedHandler()
    }
}