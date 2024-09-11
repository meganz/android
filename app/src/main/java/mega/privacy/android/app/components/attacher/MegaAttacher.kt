package mega.privacy.android.app.components.attacher

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.di.notification.getMonitorStorageStateEvent
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.AttachNodeToChatListener
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.interfaces.showSnackbarWithChat
import mega.privacy.android.app.listeners.AttachNodesListener
import mega.privacy.android.app.listeners.CreateChatListener
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants.NODE_HANDLES
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_CHAT
import mega.privacy.android.app.utils.Constants.SELECTED_CHATS
import mega.privacy.android.app.utils.Constants.SELECTED_CONTACTS
import mega.privacy.android.app.utils.Constants.SELECTED_USERS
import mega.privacy.android.app.utils.Constants.USER_HANDLES
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUser


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

    val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase =
        getMonitorStorageStateEvent()

    /**
     * Record if an attach is ongoing.
     */
    private var attaching = false

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
        snackbarShower: SnackbarShower,
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

        processContactsAndChats(chatIds, contactEmails) { managedChatIds ->
            if (managedChatIds.isEmpty()) {
                showAttachFailSnackbar(
                    nodeHandlesToAttach, contactHandlesToAttach,
                    (chatIds?.size ?: 0) + contactEmails.size, snackbarShower
                )

                attaching = false
                return@processContactsAndChats
            }

            when {
                nodeHandlesToAttach != null && nodeHandlesToAttach.isNotEmpty() -> {
                    attachNodesToChats(nodeHandlesToAttach, managedChatIds, snackbarShower)
                }

                contactHandlesToAttach != null && contactHandlesToAttach.isNotEmpty() -> {
                    attachContactsToChats(contactHandlesToAttach, managedChatIds, snackbarShower)
                }

                else -> {
                    attaching = false
                }
            }
        }

        return true
    }

    /**
     * Show attach fail snackbar.
     *
     * @param nodeHandles node handles if it's attaching nodes
     * @param contactHandles contact handles if it's attaching contacts
     * @param totalChats total chats number
     * @param snackbarShower interface to show snackbar
     */
    private fun showAttachFailSnackbar(
        nodeHandles: LongArray?,
        contactHandles: LongArray?,
        totalChats: Int,
        snackbarShower: SnackbarShower,
    ) {
        when {
            nodeHandles != null && nodeHandles.isNotEmpty() -> {
                snackbarShower.showSnackbar(
                    app.resources.getQuantityString(
                        R.plurals.num_files_not_send,
                        nodeHandles.size,
                        totalChats
                    )
                )
            }

            contactHandles != null && contactHandles.isNotEmpty() -> {
                snackbarShower.showSnackbar(
                    app.resources.getQuantityString(
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
     * @param contactEmail email of the contact to attach nodes to
     * @param snackbarShower interface to show snackbar
     */
    fun handleSelectFileResult(
        data: Intent?,
        contactEmail: String,
        snackbarShower: SnackbarShower,
    ) {
        if (data == null) {
            return
        }

        data.putStringArrayListExtra(SELECTED_CONTACTS, arrayListOf(contactEmail))

        handleSelectFileResult(data, snackbarShower)
    }


    /**
     * Handle activity result of REQUEST_CODE_SELECT_FILE.
     *
     * @param data data of onActivityResult
     * @param snackbarShower interface to show snackbar
     */
    fun handleSelectFileResult(
        data: Intent?,
        snackbarShower: SnackbarShower,
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
            val chatRoom = megaChatApi.getChatRoomByUser(contact?.handle ?: MEGACHAT_INVALID_HANDLE)
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
                    app.resources.getQuantityString(
                        R.plurals.num_files_not_send,
                        nodeHandles.size,
                        totalChats
                    )
                )
            }
        }
    }

    /**
     * Process contacts and chats of the select result, so we can attach nodes or contacts to them.
     *
     * If there are contacts without chat, we will create chat before attach. The full chat id
     * array will be provided in the callback parameter.
     *
     * @param chatIds the id of existing chats
     * @param contactEmails the email of no chat contacts
     * @param onFinish the callback for process finished
     */
    private fun processContactsAndChats(
        chatIds: LongArray?,
        contactEmails: List<String>,
        onFinish: (List<Long>) -> Unit,
    ) {
        if (contactEmails.isNotEmpty()) {
            val users = ArrayList<MegaUser>()

            for (email in contactEmails) {
                val user = megaApi.getContact(email)

                if (user != null) {
                    users.add(user)
                }
            }

            val listener = CreateChatListener(
                app, CreateChatListener.ATTACH, users.size
            ) { successChats, _ ->
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

    /**
     * Attach nodes to chats after check if all nodes are mine.
     *
     * @param handles handle of nodes to attach
     * @param chatIds id of chats to attach
     * @param snackbarShower interface to show snackbar
     * @param forceNonChatSnackbar if we should show snackbar without chat id
     * @param attachNodeToChatListener optional listener for attach
     */
    private fun attachNodesToChats(
        handles: LongArray,
        chatIds: List<Long>,
        snackbarShower: SnackbarShower,
        forceNonChatSnackbar: Boolean = false,
        attachNodeToChatListener: AttachNodeToChatListener? = null,
    ) {
        if (monitorStorageStateEventUseCase.getState() == StorageState.PayWall) {
            AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
            return
        }

        val ownerNodes = ArrayList<MegaNode>()
        val notOwnerNodes = ArrayList<MegaNode>()

        val nodes = ArrayList<MegaNode>()
        for (handle in handles) {
            val node = megaApi.getNodeByHandle(handle);

            if (node != null) {
                nodes.add(node)
            }
        }

        NodeController(app).checkIfNodesAreMine(nodes, ownerNodes, notOwnerNodes)

        val listener = AttachNodesListener(
            handles.size * chatIds.size,
            if (chatIds.size == 1) chatIds[0] else MEGACHAT_INVALID_HANDLE,
            snackbarShower, forceNonChatSnackbar, attachNodeToChatListener
        ) {
            attaching = false
        }

        if (notOwnerNodes.isEmpty()) {
            attachNodesToChats(chatIds, ownerNodes, listener)
            return
        }

        AttachmentsCopier(megaApi = megaApi, nodes = nodes, context = app) { successNodes, _ ->
            val nodesToAttach = ownerNodes + successNodes
            if (nodesToAttach.isNotEmpty()) {
                attachNodesToChats(chatIds, nodesToAttach, listener)
            } else {
                attaching = false
            }
        }
    }

    /**
     * Attach nodes to chats.
     *
     * @param chatIds  Identifier of chats to attach.
     * @param nodes    List of nodes to attach.
     * @param listener Listener to attach.
     */
    private fun attachNodesToChats(
        chatIds: List<Long>,
        nodes: List<MegaNode>,
        listener: AttachNodesListener,
    ) {
        for (chatId in chatIds) {
            for (node in nodes) {
                megaChatApi.attachNode(chatId, node.handle, listener)
            }
        }
    }

    /**
     * Attach contacts to chats.
     *
     * @param contactHandles handle of contacts to attach
     * @param chatIds id of chats to attach
     * @param snackbarShower interface to show snackbar
     */
    private fun attachContactsToChats(
        contactHandles: LongArray,
        chatIds: List<Long>,
        snackbarShower: SnackbarShower,
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

    companion object {
        private const val STATE_KEY_ATTACHING = "attaching"
    }
}
