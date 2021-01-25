package mega.privacy.android.app.components.attacher

import android.app.Activity.RESULT_OK
import android.content.Intent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.AttachNodesListener
import mega.privacy.android.app.listeners.CreateChatsListener
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants.*
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.util.*

/**
 * A class that encapsulate all the procedure of send nodes to chats,
 * include checking over disk quota, copying nodes if they are not owned yet.
 *
 * It simplifies code in activity/fragment where nodes need to be sent to chats.
 */
class NodeAttacher(private val activityLauncher: ActivityLauncher) {
    private val app = MegaApplication.getInstance()
    private val megaApi = app.megaApi
    private val megaChatApi = app.megaChatApi

    fun attachNode(handle: Long) {
        val node = megaApi.getNodeByHandle(handle) ?: return

        attachNodes(listOf(node))
    }

    fun attachNode(node: MegaNode) {
        attachNodes(listOf(node))
    }

    fun attachNodes(nodes: List<MegaNode>) {
        if (app.storageState == MegaApiJava.STORAGE_STATE_PAYWALL) {
            AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
            return
        }

        val ownerNodes = ArrayList<MegaNode>()
        val notOwnerNodes = ArrayList<MegaNode>()
        NodeController(app).checkIfNodesAreMine(nodes, ownerNodes, notOwnerNodes)

        if (notOwnerNodes.isEmpty()) {
            selectChatsToAttach(ownerNodes)
            return
        }

        AttachmentsCopier(nodes) { successNodes, failureCount ->
            if (failureCount == 0) {
                selectChatsToAttach(ownerNodes + successNodes)
            } else {
                // TODO?
            }
        }
    }

    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        snackbarShower: SnackbarShower
    ): Boolean {
        if (requestCode != REQUEST_CODE_SELECT_CHAT || resultCode != RESULT_OK || data == null) {
            return false
        }

        val nodeHandles = data.getLongArrayExtra(NODE_HANDLES)
        if (nodeHandles == null || nodeHandles.isEmpty()) {
            return false
        }

        val chatHandles = data.getLongArrayExtra(SELECTED_CHATS)
        val contactHandles = data.getLongArrayExtra(SELECTED_USERS)

        if (contactHandles != null && contactHandles.isNotEmpty()) {
            val users = ArrayList<MegaUser>()

            for (handle in contactHandles) {
                val user = megaApi.getContact(MegaApiAndroid.userHandleToBase64(handle))

                if (user != null) {
                    users.add(user)
                }
            }

            val listener = CreateChatsListener(users.size) { successChats, failureCount ->
                if (failureCount == 0) {
                    val chatIds = if (chatHandles != null && chatHandles.isNotEmpty()) {
                        chatHandles.toList() + successChats
                    } else {
                        successChats
                    }

                    attachNodesToChats(nodeHandles, chatIds, snackbarShower)
                } else {
                    // TODO?
                }
            }

            for (user in users) {
                val peers = MegaChatPeerList.createInstance()
                peers.addPeer(user.handle, MegaChatPeerList.PRIV_STANDARD)
                megaChatApi.createChat(false, peers, listener)
            }
        } else if (chatHandles != null && chatHandles.isNotEmpty()) {
            attachNodesToChats(nodeHandles, chatHandles.toList(), snackbarShower)
        }

        return true
    }

    private fun selectChatsToAttach(nodes: List<MegaNode>) {
        val handles = LongArray(nodes.size)

        for (i in nodes.indices) {
            handles[i] = nodes[i].handle
        }

        val intent = Intent(app, ChatExplorerActivity::class.java)
        intent.putExtra(NODE_HANDLES, handles)

        activityLauncher.launchActivityForResult(intent, REQUEST_CODE_SELECT_CHAT)
    }

    private fun attachNodesToChats(
        handles: LongArray,
        chatIds: List<Long>,
        snackbarShower: SnackbarShower
    ) {
        val listener = AttachNodesListener(
            handles.size * chatIds.size,
            if (chatIds.size == 1) chatIds[0] else MEGACHAT_INVALID_HANDLE,
            snackbarShower
        )

        for (chatId in chatIds) {
            for (handle in handles) {
                megaChatApi.attachNode(chatId, handle, listener)
            }
        }
    }
}
