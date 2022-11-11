package mega.privacy.android.app.listeners

import nz.mega.sdk.*

/**
 * MegaGlobalListenerInterface with optional callbacks.
 */
class OptionalMegaGlobalListenerInterface(
    private val onUsersUpdate: ((ArrayList<MegaUser>?) -> Unit)? = null,
    private val onUserAlertsUpdate: ((ArrayList<MegaUserAlert>?) -> Unit)? = null,
    private val onNodesUpdate: ((ArrayList<MegaNode>?) -> Unit)? = null,
    private val onReloadNeeded: (() -> Unit)? = null,
    private val onAccountUpdate: (() -> Unit)? = null,
    private val onContactRequestsUpdate: ((ArrayList<MegaContactRequest>?) -> Unit)? = null,
    private val onEvent: ((MegaEvent) -> Unit)? = null,
    private val onSetsUpdate: ((ArrayList<MegaSet>?) -> Unit)? = null,
    private val onSetElementsUpdate: ((ArrayList<MegaSetElement>?) -> Unit)? = null
) : MegaGlobalListenerInterface {

    override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser>?) {
        onUsersUpdate?.invoke(users)
    }

    override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert>?) {
        onUserAlertsUpdate?.invoke(userAlerts)
    }

    override fun onNodesUpdate(api: MegaApiJava, nodeList: ArrayList<MegaNode>?) {
        onNodesUpdate?.invoke(nodeList)
    }

    override fun onReloadNeeded(api: MegaApiJava) {
        onReloadNeeded?.invoke()
    }

    override fun onAccountUpdate(api: MegaApiJava) {
        onAccountUpdate?.invoke()
    }

    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest>?
    ) {
        onContactRequestsUpdate?.invoke(requests)
    }

    override fun onEvent(api: MegaApiJava, event: MegaEvent) {
        onEvent?.invoke(event)
    }

    override fun onSetsUpdate(api: MegaApiJava?, sets: ArrayList<MegaSet>?) {
        onSetsUpdate?.invoke(sets)
    }

    override fun onSetElementsUpdate(
        api: MegaApiJava?,
        elements: ArrayList<MegaSetElement>?,
    ) {
        onSetElementsUpdate?.invoke(elements)
    }
}
