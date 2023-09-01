package mega.privacy.android.data.listener

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaListenerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncStats
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert

/**
 * MegaListenerInterface with optional callbacks
 *
 * Currently, the only optional callback is onSyncDeleted, because other callbacks are covered by
 * [OptionalMegaRequestListenerInterface], [OptionalMegaTransferListenerInterface]
 */
class OptionalMegaListenerInterface(
    private val onSyncDeleted: ((sync: MegaSync) -> Unit)? = null,
    private val onSyncStatsUpdated: ((syncStats: MegaSyncStats) -> Unit)? = null,
    private val onSyncStateChanged: ((sync: MegaSync) -> Unit)? = null,
) : MegaListenerInterface {

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {

    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {

    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {

    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {

    }

    override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser>?) {

    }

    override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert>?) {

    }

    override fun onNodesUpdate(api: MegaApiJava, nodeList: ArrayList<MegaNode>?) {

    }

    override fun onReloadNeeded(api: MegaApiJava) {

    }

    override fun onAccountUpdate(api: MegaApiJava) {

    }

    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest>?,
    ) {

    }

    override fun onEvent(api: MegaApiJava, event: MegaEvent?) {

    }

    override fun onSetsUpdate(api: MegaApiJava, sets: ArrayList<MegaSet>?) {

    }

    override fun onSetElementsUpdate(api: MegaApiJava, elements: ArrayList<MegaSetElement>?) {

    }

    override fun onGlobalSyncStateChanged(api: MegaApiJava) {

    }

    override fun onTransferStart(api: MegaApiJava, transfer: MegaTransfer) {

    }

    override fun onTransferFinish(api: MegaApiJava, transfer: MegaTransfer, e: MegaError) {

    }

    override fun onTransferUpdate(api: MegaApiJava, transfer: MegaTransfer) {

    }

    override fun onTransferTemporaryError(api: MegaApiJava, transfer: MegaTransfer, e: MegaError) {

    }

    override fun onTransferData(
        api: MegaApiJava,
        transfer: MegaTransfer,
        buffer: ByteArray,
    ): Boolean {
        return false
    }

    override fun onSyncDeleted(api: MegaApiJava, sync: MegaSync) {
        onSyncDeleted?.invoke(sync)
    }

    override fun onSyncStatsUpdated(api: MegaApiJava, syncStats: MegaSyncStats) {
        onSyncStatsUpdated?.invoke(syncStats)
    }

    override fun onSyncStateChanged(api: MegaApiJava, sync: MegaSync) {
        onSyncStateChanged?.invoke(sync)
    }
}