package mega.privacy.android.app.components.attacher

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.*
import mega.privacy.android.app.listeners.AttachNodesListener
import mega.privacy.android.app.listeners.CreateChatsListener
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE


/**
 * A class that encapsulate all the procedure of send nodes/contacts/link to chats,
 * include avoiding trigger select chat screen while still attaching,
 * checking over disk quota, copying nodes if they are not owned yet.
 *
 * It simplifies code in activity/fragment where nodes/contacts/link need to be sent to chats.
 *
 * @param activityLauncher interface to start activity
 */
class MegaAttacher(private val activityLauncher: ActivityLauncher) {
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

        AttachmentsCopier(nodes) { successNodes, _ ->
            val nodesToAttach = ownerNodes + successNodes
            if (nodesToAttach.isNotEmpty()) {
                selectChatsToAttach(nodesToAttach)
            } else {
                attaching = false
            }
        }
    }

    /**
     * Save instance state, should be called from onSaveInstanceState of the owning
     * activity/fragment.
     *
     * @param outState outState param of onSaveInstanceState
     */
    fun saveState(outState: Bundle) {
        outState.putBoolean(STATE_KEY_ATTACHING, attaching)
    }

    /**
     * Restore instance state, should be called from onCreate of the owning
     * activity/fragment.
     *
     * @param savedInstanceState savedInstanceState param of onCreate
     */
    fun restoreState(savedInstanceState: Bundle) {
        attaching = savedInstanceState.getBoolean(STATE_KEY_ATTACHING, false)
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

        val chatIds = data.getLongArrayExtra(SELECTED_CHATS)
        val contactHandles = data.getLongArrayExtra(SELECTED_USERS)

        val contactEmails = ArrayList<String>()
        if (contactHandles != null && contactHandles.isNotEmpty()) {
            for (handle in contactHandles) {
                contactEmails.add(MegaApiAndroid.userHandleToBase64(handle))
            }
        }

        val nodeHandlesToAttach = data.getLongArrayExtra(NODE_HANDLES)
        val contactHandlesToAttach = data.getLongArrayExtra(USER_HANDLES)
        val linkToAttach = data.getStringExtra(EXTRA_LINK)
        val linkKey = data.getStringExtra(EXTRA_KEY)
        val linkPassword = data.getStringExtra(EXTRA_PASSWORD)

        processContactsAndChats(chatIds, contactEmails) {
            if (it.isEmpty()) {
                showAttachFailSnackbar(
                    nodeHandlesToAttach, contactHandlesToAttach,
                    (chatIds?.size ?: 0) + contactEmails.size, snackbarShower
                )

                attaching = false
                return@processContactsAndChats
            }

            when {
                nodeHandlesToAttach != null && nodeHandlesToAttach.isNotEmpty() -> {
                    attachNodesToChats(nodeHandlesToAttach, it, snackbarShower)
                }
                contactHandlesToAttach != null && contactHandlesToAttach.isNotEmpty() -> {
                    attachContactsToChats(contactHandlesToAttach, it, snackbarShower)
                }
                !TextUtils.isEmpty(linkToAttach) -> {
                    attachLinkToChats(linkToAttach!!, linkKey, linkPassword, it, snackbarShower)
                }
                else -> {
                    attaching = false
                }
            }
        }

        return true
    }

    private fun showAttachFailSnackbar(
        nodeHandles: LongArray?,
        contactHandles: LongArray?,
        totalChats: Int,
        snackbarShower: SnackbarShower
    ) {
        when {
            nodeHandles != null && nodeHandles.isNotEmpty() -> {
                snackbarShower.showSnackbar(
                    getQuantityString(R.plurals.num_files_not_send, nodeHandles.size, totalChats)
                )
            }
            contactHandles != null && contactHandles.isNotEmpty() -> {
                snackbarShower.showSnackbar(
                    getQuantityString(
                        R.plurals.num_contacts_not_send, contactHandles.size, totalChats
                    )
                )
            }
        }
    }

    /**
     * Handle activity result of REQUEST_CODE_SELECT_FILE.
     *
     * @param data data of onActivityResult
     * @param contact contact to attach nodes to
     * @param snackbarShower interface to show snackbar
     */
    fun handleSelectFileResult(
        data: Intent?,
        contact: MegaUser,
        snackbarShower: SnackbarShower
    ) {
        if (data == null) {
            return
        }

        data.putStringArrayListExtra(SELECTED_CONTACTS, arrayListOf(contact.email))

        handleSelectFileResult(data, snackbarShower)
    }

    /**
     * Handle activity result of REQUEST_CODE_SELECT_FILE.
     *
     * @param data data of onActivityResult
     * @param chatId chat id to attach nodes to
     * @param snackbarShower interface to show snackbar
     * @param attachNodeToChatListener interface for attached message
     */
    fun handleSelectFileResult(
        data: Intent?,
        chatId: Long,
        snackbarShower: SnackbarShower,
        attachNodeToChatListener: AttachNodeToChatListener
    ) {
        val nodeHandles = data?.getLongArrayExtra(NODE_HANDLES) ?: return
        if (nodeHandles.isEmpty()) {
            return
        }

        val chatIds = longArrayOf(chatId)
        processContactsAndChats(chatIds, emptyList()) {
            if (it.isNotEmpty()) {
                attachNodesToChats(nodeHandles, it, snackbarShower, true, attachNodeToChatListener)
            }
        }
    }

    /**
     * Handle activity result of REQUEST_CODE_SELECT_FILE.
     *
     * @param data data of onActivityResult
     * @param snackbarShower interface to show snackbar
     */
    fun handleSelectFileResult(
        data: Intent?,
        snackbarShower: SnackbarShower
    ) {
        val nodeHandles = data?.getLongArrayExtra(NODE_HANDLES) ?: return
        if (nodeHandles.isEmpty()) {
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

        val contactsWithoutChat = contactEmails - contactsWithChatRoom
        val totalChats = (chatIdsArray?.size ?: 0) + contactsWithoutChat.size

        processContactsAndChats(chatIdsArray, contactsWithoutChat) {
            if (it.isNotEmpty()) {
                attachNodesToChats(nodeHandles, it, snackbarShower)
            } else {
                snackbarShower.showSnackbar(
                    getQuantityString(R.plurals.num_files_not_send, nodeHandles.size, totalChats)
                )
            }
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

    private fun processContactsAndChats(
        chatIds: LongArray?,
        contactEmails: List<String>,
        onFinish: (List<Long>) -> Unit
    ) {
        if (contactEmails.isNotEmpty()) {
            val users = ArrayList<MegaUser>()

            for (email in contactEmails) {
                val user = megaApi.getContact(email)

                if (user != null) {
                    users.add(user)
                }
            }

            val listener = CreateChatsListener(users.size) { successChats, _ ->
                val chatIdsList = if (chatIds != null && chatIds.isNotEmpty()) {
                    chatIds.toList() + successChats
                } else {
                    successChats
                }

                if (chatIdsList.isNotEmpty()) {
                    onFinish(chatIdsList)
                } else {
                    onFinish(emptyList())
                }
            }

            for (user in users) {
                val peers = MegaChatPeerList.createInstance()
                peers.addPeer(user.handle, MegaChatPeerList.PRIV_STANDARD)
                megaChatApi.createChat(false, peers, listener)
            }
        } else if (chatIds != null && chatIds.isNotEmpty()) {
            onFinish(chatIds.toList())
        }
    }

    private fun attachNodesToChats(
        handles: LongArray,
        chatIds: List<Long>,
        snackbarShower: SnackbarShower,
        forceNonChatSnackbar: Boolean = false,
        attachNodeToChatListener: AttachNodeToChatListener? = null
    ) {
        val listener = AttachNodesListener(
            handles.size * chatIds.size,
            if (chatIds.size == 1) chatIds[0] else MEGACHAT_INVALID_HANDLE,
            snackbarShower, forceNonChatSnackbar, attachNodeToChatListener
        ) {
            attaching = false
        }

        for (chatId in chatIds) {
            for (handle in handles) {
                megaChatApi.attachNode(chatId, handle, listener)
            }
        }
    }

    private fun attachContactsToChats(
        contactHandles: LongArray,
        chatIds: List<Long>,
        snackbarShower: SnackbarShower
    ) {
        val handleList = MegaHandleList.createInstance()
        for (handle in contactHandles) {
            handleList.addMegaHandle(handle)
        }

        for (chatId in chatIds) {
            megaChatApi.attachContacts(chatId, handleList)
        }

        snackbarShower.showSnackbarWithChat(
            null, if (chatIds.size == 1) chatIds[0] else MEGACHAT_INVALID_HANDLE
        )
    }

    private fun attachLinkToChats(
        link: String,
        key: String?,
        password: String?,
        chatIds: List<Long>,
        snackbarShower: SnackbarShower
    ) {
        for (chatId in chatIds) {
            megaChatApi.sendMessage(chatId, link)
            if (!TextUtils.isEmpty(key)) {
                megaChatApi.sendMessage(chatId, key)
            } else if (!TextUtils.isEmpty(password)) {
                megaChatApi.sendMessage(chatId, password)
            }
        }

        val message = if (!TextUtils.isEmpty(key)) {
            getString(R.string.link_and_key_sent)
        } else if (!TextUtils.isEmpty(password)) {
            getString(R.string.link_and_password_sent)
        } else {
            getString(R.string.link_sent)
        }

        snackbarShower.showSnackbarWithChat(
            message, if (chatIds.size == 1) chatIds[0] else MEGACHAT_INVALID_HANDLE
        )
    }

    companion object {
        private const val STATE_KEY_ATTACHING = "attaching"
    }
}
