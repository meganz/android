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


/**
 * A class that encapsulate all the procedure of send nodes to chats,
 * include avoiding trigger select chat screen while still attaching,
 * checking over disk quota, copying nodes if they are not owned yet.
 *
 * It simplifies code in activity/fragment where nodes need to be sent to chats.
 *
 * @param activityLauncher interface to start activity
 */
class NodeAttacher(private val activityLauncher: ActivityLauncher) {
    private val app = MegaApplication.getInstance()
    private val megaApi = app.megaApi
    private val megaChatApi = app.megaChatApi

    /**
     * Record if an attach is ongoing.
     */
    private var attaching = false

    /**
     * Attach a node to chats.
     *
     * @param handle handle of the node
     */
    fun attachNode(handle: Long) {
        val node = megaApi.getNodeByHandle(handle) ?: return

        attachNodes(listOf(node))
    }

    /**
     * Attach a node to chats.
     *
     * @param node node to attach
     */
    fun attachNode(node: MegaNode) {
        attachNodes(listOf(node))
    }

    /**
     * Attach nodes to chats.
     *
     * @param nodes nodes to attach
     */
    fun attachNodes(nodes: List<MegaNode>) {
        if (attaching) {
            return
        }

        if (app.storageState == MegaApiJava.STORAGE_STATE_PAYWALL) {
            AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
            return
        }

        val ownerNodes = ArrayList<MegaNode>()
        val notOwnerNodes = ArrayList<MegaNode>()
        NodeController(app).checkIfNodesAreMine(nodes, ownerNodes, notOwnerNodes)

        attaching = true

        if (notOwnerNodes.isEmpty()) {
            selectChatsToAttach(ownerNodes)
            return
        }

        AttachmentsCopier(nodes) { successNodes, failureCount ->
            if (failureCount == 0) {
                selectChatsToAttach(ownerNodes + successNodes)
            } else {
                // TODO?
                attaching = false
            }
        }
    }

    /**
     * Handle activity result launched by NodeAttacher.
     *
     * @param requestCode requestCode of onActivityResult
     * @param resultCode resultCode of onActivityResult
     * @param data data of onActivityResult
     * @param snackbarShower interface to show snackbar
     */
    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        snackbarShower: SnackbarShower
    ): Boolean {
        if (requestCode != REQUEST_CODE_SELECT_CHAT) {
            return false
        }

        if (resultCode != RESULT_OK || data == null) {
            attaching = false
            return true
        }

        val nodeHandles = data.getLongArrayExtra(NODE_HANDLES)
        if (nodeHandles == null || nodeHandles.isEmpty()) {
            attaching = false
            return true
        }

        val chatIds = data.getLongArrayExtra(SELECTED_CHATS)
        val contactHandles = data.getLongArrayExtra(SELECTED_USERS)

        val contactEmails = ArrayList<String>()
        if (contactHandles != null && contactHandles.isNotEmpty()) {
            for (handle in contactHandles) {
                contactEmails.add(MegaApiAndroid.userHandleToBase64(handle))
            }
        }

        doHandleActivityResult(nodeHandles, chatIds, contactEmails, snackbarShower)

        return true
    }

    /**
     * Handle activity result of REQUEST_CODE_SELECT_FILE.
     *
     * @param requestCode requestCode of onActivityResult
     * @param resultCode resultCode of onActivityResult
     * @param data data of onActivityResult
     * @param snackbarShower interface to show snackbar
     */
    fun handleSelectFileResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        snackbarShower: SnackbarShower
    ) {
        if (requestCode != REQUEST_CODE_SELECT_FILE || resultCode != RESULT_OK || data == null) {
            return
        }

        val nodeHandles = data.getLongArrayExtra(NODE_HANDLES)
        if (nodeHandles == null || nodeHandles.isEmpty()) {
            return
        }

        val contactEmails = data.getStringArrayListExtra(SELECTED_CONTACTS) ?: ArrayList<String>()
        val contactsWithChatRoom = ArrayList<String>()
        val chatIds = ArrayList<Long>()

        for (email in contactEmails) {
            val contact = megaApi.getContact(email)
            val chatRoom = megaChatApi.getChatRoomByUser(contact.handle)
            if (chatRoom != null) {
                chatIds.add(chatRoom.chatId)
                contactsWithChatRoom.add(email)
            }
        }

        val chatIdsArray = if (chatIds.isEmpty()) {
            null
        } else {
            val array = LongArray(chatIds.size)

            for (i in chatIds.indices) {
                array[i] = chatIds[i]
            }

            array
        }

        doHandleActivityResult(
            nodeHandles, chatIdsArray, contactEmails - contactsWithChatRoom, snackbarShower
        )
    }

    private fun doHandleActivityResult(
        nodeHandles: LongArray,
        chatIds: LongArray?,
        contactEmails: List<String>,
        snackbarShower: SnackbarShower
    ) {
        if (contactEmails.isNotEmpty()) {
            val users = ArrayList<MegaUser>()

            for (email in contactEmails) {
                val user = megaApi.getContact(email)

                if (user != null) {
                    users.add(user)
                }
            }

            val listener = CreateChatsListener(users.size) { successChats, failureCount ->
                if (failureCount == 0) {
                    val chatIds = if (chatIds != null && chatIds.isNotEmpty()) {
                        chatIds.toList() + successChats
                    } else {
                        successChats
                    }

                    attachNodesToChats(nodeHandles, chatIds, snackbarShower)
                } else {
                    // TODO?
                    attaching = false
                }
            }

            for (user in users) {
                val peers = MegaChatPeerList.createInstance()
                peers.addPeer(user.handle, MegaChatPeerList.PRIV_STANDARD)
                megaChatApi.createChat(false, peers, listener)
            }
        } else if (chatIds != null && chatIds.isNotEmpty()) {
            attachNodesToChats(nodeHandles, chatIds.toList(), snackbarShower)
        } else {
            attaching = false
        }
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
        ) {
            attaching = false
        }

        for (chatId in chatIds) {
            for (handle in handles) {
                megaChatApi.attachNode(chatId, handle, listener)
            }
        }
    }
}
