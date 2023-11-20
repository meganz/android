package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.R
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.controllers.ContactController
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.main.megachat.ChatReactionsView
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.openWith
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.showCannotOpenFileDialog
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.data.model.chat.AndroidMegaChatMessage
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatContainsMeta
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject

/**
 * GeneralChatMessageBottomSheet
 * @property getNodeUseCase [GetNodeUseCase]
 */
@AndroidEntryPoint
class GeneralChatMessageBottomSheet : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private var node: MegaNode? = null
    private var nodeList: MegaNodeList? = null
    private var message: AndroidMegaChatMessage? = null
    private var chatId: Long = 0
    private var messageId: Long = 0
    private var positionMessage = 0
    private var handle = MegaApiJava.INVALID_HANDLE
    private lateinit var chatC: ChatController
    private var chatRoom: MegaChatRoom? = null
    private lateinit var reactionsLayout: LinearLayout
    private lateinit var reactionsFragment: ChatReactionsView
    private lateinit var optionOpenWith: RelativeLayout
    private lateinit var reactionSeparator: View
    private lateinit var optionForward: RelativeLayout
    private lateinit var optionEdit: RelativeLayout
    private lateinit var optionCopy: RelativeLayout
    private lateinit var optionShare: RelativeLayout
    private lateinit var optionSelect: RelativeLayout
    private lateinit var optionViewContacts: RelativeLayout
    private lateinit var optionInfoContacts: RelativeLayout
    private lateinit var optionStartConversation: RelativeLayout
    private lateinit var optionInviteContact: RelativeLayout
    private lateinit var optionImport: RelativeLayout
    private lateinit var optionDownload: RelativeLayout
    private lateinit var offlineSwitch: SwitchMaterial
    private lateinit var optionDelete: RelativeLayout
    private lateinit var forwardSeparator: LinearLayout
    private lateinit var editSeparator: LinearLayout
    private lateinit var copySeparator: LinearLayout
    private lateinit var shareSeparator: LinearLayout
    private lateinit var selectSeparator: LinearLayout
    private lateinit var infoSeparator: LinearLayout
    private lateinit var inviteSeparator: LinearLayout
    private lateinit var infoFileSeparator: LinearLayout
    private lateinit var optionSaveOffline: LinearLayout
    private lateinit var deleteSeparator: LinearLayout
    private var cannotOpenFileDialog: AlertDialog? = null
    private val rxSubscriptions = CompositeDisposable()

    @Inject
    lateinit var getNodeUseCase: GetNodeUseCase

    /**
     * On create view event
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        contentView = View.inflate(context, R.layout.bottom_sheet_general_chat_messages, null)
        reactionsLayout = contentView.findViewById(R.id.reactions_layout)
        reactionsFragment = contentView.findViewById(R.id.fragment_container_reactions)
        itemsLayout = contentView.findViewById(R.id.items_layout)
        if (savedInstanceState != null) {
            Timber.d("Bundle is NOT NULL")
            chatId = savedInstanceState.getLong(Constants.CHAT_ID, Constants.INVALID_ID.toLong())
            messageId =
                savedInstanceState.getLong(Constants.MESSAGE_ID, Constants.INVALID_ID.toLong())
            positionMessage = savedInstanceState.getInt(
                Constants.POSITION_SELECTED_MESSAGE,
                Constants.INVALID_POSITION
            )
            handle = savedInstanceState.getLong(Constants.HANDLE, MegaApiJava.INVALID_HANDLE)
        } else {
            chatId = (requireActivity() as ChatActivity).idChat
            messageId = (requireActivity() as ChatActivity).selectedMessageId
            positionMessage = (requireActivity() as ChatActivity).selectedPosition
        }
        val messageMega = megaChatApi.getMessage(chatId, messageId)
        if (messageMega != null) {
            message = AndroidMegaChatMessage(messageMega)
        }
        chatRoom = megaChatApi.getChatRoom(chatId)
        chatC = ChatController(requireActivity())
        return contentView
    }

    /**
     * On view created event
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        reactionSeparator = contentView.findViewById(R.id.separator)
        optionOpenWith = contentView.findViewById(R.id.open_with_layout)
        forwardSeparator = contentView.findViewById(R.id.forward_separator)
        optionForward = contentView.findViewById(R.id.forward_layout)
        editSeparator = contentView.findViewById(R.id.edit_separator)
        optionEdit = contentView.findViewById(R.id.edit_layout)
        copySeparator = contentView.findViewById(R.id.copy_separator)
        optionCopy = contentView.findViewById(R.id.copy_layout)
        shareSeparator = contentView.findViewById(R.id.share_separator)
        optionShare = contentView.findViewById(R.id.share_layout)
        selectSeparator = contentView.findViewById(R.id.select_separator)
        optionSelect = contentView.findViewById(R.id.select_layout)
        infoSeparator = contentView.findViewById(R.id.info_separator)
        optionViewContacts = contentView.findViewById(R.id.option_view_layout)
        optionInfoContacts = contentView.findViewById(R.id.option_info_layout)
        inviteSeparator = contentView.findViewById(R.id.invite_separator)
        optionStartConversation = contentView.findViewById(R.id.option_start_conversation_layout)
        optionInviteContact = contentView.findViewById(R.id.option_invite_layout)
        infoFileSeparator = contentView.findViewById(R.id.info_file_separator)
        optionImport = contentView.findViewById(R.id.option_import_layout)
        optionDownload = contentView.findViewById(R.id.option_download_layout)
        optionSaveOffline = contentView.findViewById(R.id.option_save_offline_layout)
        offlineSwitch = contentView.findViewById(R.id.file_properties_switch)
        deleteSeparator = contentView.findViewById(R.id.delete_separator)
        optionDelete = contentView.findViewById(R.id.delete_layout)
        val textDelete = contentView.findViewById<TextView>(R.id.delete_text)
        optionOpenWith.setOnClickListener(this)
        optionForward.setOnClickListener(this)
        optionEdit.setOnClickListener(this)
        optionCopy.setOnClickListener(this)
        optionShare.setOnClickListener(this)
        optionSelect.setOnClickListener(this)
        optionViewContacts.setOnClickListener(this)
        optionInfoContacts.setOnClickListener(this)
        optionStartConversation.setOnClickListener(this)
        optionInviteContact.setOnClickListener(this)
        optionImport.setOnClickListener(this)
        optionDownload.setOnClickListener(this)
        optionSaveOffline.setOnClickListener(this)
        optionDelete.setOnClickListener(this)
        val megaChatMessage: MegaChatMessage? = message?.message
        if (megaChatMessage == null || chatRoom == null || message?.isUploading == true) {
            Timber.w("Message is null")
            closeDialog()
            return
        }
        val isRemovedOrPendingMsg =
            (requireActivity() as ChatActivity).hasMessagesRemovedOrPending(message?.message)
        if (isRemovedOrPendingMsg) {
            Timber.w("Message is removed or pending")
            closeDialog()
            return
        }
        hideAllOptions()
        checkReactionsFragment()
        val typeMessage = megaChatMessage.type
        optionSelect.visibility = View.VISIBLE
        if (typeMessage == MegaChatMessage.TYPE_NORMAL || ChatUtil.isGeolocation(megaChatMessage) || typeMessage == MegaChatMessage.TYPE_CONTAINS_META && megaChatMessage.containsMeta != null && megaChatMessage.containsMeta.type == MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW) {
            optionCopy.visibility = View.VISIBLE
        } else {
            optionCopy.visibility = View.GONE
        }
        if ((chatRoom?.ownPrivilege == MegaChatRoom.PRIV_RM || chatRoom?.ownPrivilege == MegaChatRoom.PRIV_RO) && chatRoom?.isPreview == false) {
            optionForward.visibility = View.GONE
            optionEdit.visibility = View.GONE
            optionDelete.visibility = View.GONE
            optionShare.visibility = View.GONE
        } else {
            optionShare.visibility =
                if (typeMessage != MegaChatMessage.TYPE_NODE_ATTACHMENT
                    || !Util.isOnline(requireContext()) || chatC.isInAnonymousMode
                ) View.GONE else View.VISIBLE
            optionForward.visibility =
                if (!Util.isOnline(requireContext()) || chatC.isInAnonymousMode) View.GONE else View.VISIBLE
            if (megaChatMessage.userHandle != megaChatApi.myUserHandle ||
                !megaChatMessage.isEditable || typeMessage == MegaChatMessage.TYPE_CONTACT_ATTACHMENT
            ) {
                optionEdit.visibility = View.GONE
            } else {
                optionEdit.visibility = if (typeMessage == MegaChatMessage.TYPE_NORMAL ||
                    typeMessage == MegaChatMessage.TYPE_CONTAINS_META
                ) View.VISIBLE else View.GONE
            }
            if (megaChatMessage.userHandle != megaChatApi.myUserHandle || !megaChatMessage.isDeletable) {
                optionDelete.visibility = View.GONE
            } else {
                if (megaChatMessage.type == MegaChatMessage.TYPE_NORMAL || megaChatMessage.type == MegaChatMessage.TYPE_CONTAINS_META && megaChatMessage.containsMeta != null && megaChatMessage.containsMeta.type == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION) {
                    textDelete.text = getString(R.string.delete_button)
                } else {
                    textDelete.text = getString(R.string.context_remove)
                }
                optionDelete.visibility = View.VISIBLE
            }
        }
        optionOpenWith.visibility =
            if (typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT) View.VISIBLE else View.GONE
        optionDownload.visibility =
            if (typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT) View.VISIBLE else View.GONE
        optionImport.visibility =
            if (typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT && !chatC.isInAnonymousMode) View.VISIBLE else View.GONE
        if (typeMessage == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
            val userCount = megaChatMessage.usersCount
            val userHandle = megaChatMessage.getUserHandle(0)
            val userEmail = megaChatMessage.getUserEmail(0)
            optionInfoContacts.visibility = if (
                userCount == 1L
                && userHandle != megaChatApi.myUserHandle
                && megaApi.getContact(userEmail) != null
                && megaApi.getContact(userEmail)?.visibility == MegaUser.VISIBILITY_VISIBLE
            ) View.VISIBLE else View.GONE
            optionViewContacts.visibility = if (userCount > 1) View.VISIBLE else View.GONE
            if (userCount == 1L) {
                optionInviteContact.visibility = if (
                    userHandle != megaChatApi.myUserHandle
                    && (megaApi.getContact(userEmail) == null || megaApi.getContact(userEmail)?.visibility != MegaUser.VISIBILITY_VISIBLE)
                ) View.VISIBLE else View.GONE
                optionStartConversation.visibility = if (
                    userHandle != megaChatApi.myUserHandle
                    && megaApi.getContact(userEmail) != null
                    && megaApi.getContact(userEmail)?.visibility == MegaUser.VISIBILITY_VISIBLE
                    && (chatRoom?.isGroup == true || userHandle != chatRoom?.getPeerHandle(0))
                ) View.VISIBLE else View.GONE
            } else {
                optionStartConversation.visibility = View.VISIBLE
                optionInviteContact.visibility = View.GONE
                for (i in 0 until userCount) {
                    val email = megaChatMessage.getUserEmail(i)
                    val contact = megaApi.getContact(email)
                    if (contact == null || contact.visibility != MegaUser.VISIBILITY_VISIBLE) {
                        optionStartConversation.visibility = View.GONE
                        break
                    }
                }
            }
        }
        if (typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT) {
            getNode(megaChatMessage)
        } else {
            checkSeparatorsVisibility()
        }
        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * On view state restored event
     */
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        if (savedInstanceState != null && savedInstanceState.getBoolean(
                Constants.CANNOT_OPEN_FILE_SHOWN,
                false
            )
        ) {
            contentView.post {
                node?.let { node ->
                    cannotOpenFileDialog = this.showCannotOpenFileDialog(
                        requireActivity(),
                        node
                    ) { megaNode: MegaNode? ->
                        (requireActivity() as ChatActivity).saveNodeByTap(megaNode)
                    }
                }
            }
        }
        super.onViewStateRestored(savedInstanceState)
    }

    /**
     * On destroy view event
     */
    override fun onDestroyView() {
        dismissAlertDialogIfExists(cannotOpenFileDialog)
        rxSubscriptions.clear()
        super.onDestroyView()
    }

    private fun checkReactionsFragment() {
        val shouldReactionOptionBeVisible =
            ChatUtil.shouldReactionBeClicked(chatRoom) && message?.isUploading == false
        if (shouldReactionOptionBeVisible) {
            reactionsFragment.init(requireActivity(), this, chatId, messageId, positionMessage)
            reactionsLayout.visibility = View.VISIBLE
        } else {
            reactionsLayout.visibility = View.GONE
        }
        reactionSeparator.visibility =
            if (shouldReactionOptionBeVisible) View.VISIBLE else View.GONE
    }

    private fun hideAllOptions() {
        optionSaveOffline.visibility = View.GONE
        optionOpenWith.visibility = View.GONE
        optionForward.visibility = View.GONE
        optionEdit.visibility = View.GONE
        optionCopy.visibility = View.GONE
        optionShare.visibility = View.GONE
        optionSelect.visibility = View.GONE
        optionViewContacts.visibility = View.GONE
        optionInfoContacts.visibility = View.GONE
        optionStartConversation.visibility = View.GONE
        optionInviteContact.visibility = View.GONE
        optionImport.visibility = View.GONE
        optionDownload.visibility = View.GONE
        optionDelete.visibility = View.GONE
        checkSeparatorsVisibility()
    }

    /**
     * On click event
     */
    override fun onClick(view: View) {
        val message = this.message
        if (message == null) {
            Timber.w("The message is NULL")
            return
        }
        val messagesSelected = ArrayList<AndroidMegaChatMessage>()
        messagesSelected.add(message)
        val id = view.id
        if (id == R.id.open_with_layout) {
            if (node == null) {
                Timber.w("The selected node is NULL")
                return
            }
            cannotOpenFileDialog = this.openWith(
                requireActivity(),
                node
            ) { node: MegaNode? -> (requireActivity() as ChatActivity).saveNodeByTap(node) }
            return
        } else if (id == R.id.forward_layout) {
            (requireActivity() as ChatActivity).forwardMessages(messagesSelected)
        } else if (id == R.id.edit_layout) {
            (requireActivity() as ChatActivity).editMessage(messagesSelected)
        } else if (id == R.id.copy_layout) {
            val msg = message.message
            val text =
                if (ChatUtil.isGeolocation(msg)) msg?.containsMeta?.textMessage
                else (requireActivity() as ChatActivity).copyMessage(message)
            (requireActivity() as ChatActivity).copyToClipboard(text)
        } else if (id == R.id.share_layout) {
            if (node == null) {
                Timber.w("The selected node is NULL")
                return
            }
            ChatUtil.shareMsgFromChat(requireActivity(), message, chatId)
        } else if (id == R.id.select_layout) {
            (requireActivity() as ChatActivity).activateActionModeWithItem(positionMessage)
        } else if (id == R.id.option_view_layout) {
            Timber.d("View option")
            ContactUtil.openContactAttachmentActivity(requireActivity(), chatId, messageId)
        } else if (id == R.id.option_info_layout) {
            if (!Util.isOnline(requireContext())) {
                (requireActivity() as ChatActivity).showSnackbar(
                    Constants.SNACKBAR_TYPE, getString(
                        R.string.error_server_connection_problem
                    ), MegaApiJava.INVALID_HANDLE
                )
                return
            }
            val isChatRoomOpen =
                chatRoom != null && chatRoom?.isGroup == false
                        && message.message?.getUserHandle(0) == chatRoom?.getPeerHandle(0)
            ContactUtil.openContactInfoActivity(
                requireActivity(),
                message.message?.getUserEmail(0),
                isChatRoomOpen
            )
        } else if (id == R.id.option_invite_layout) {
            if (!Util.isOnline(requireContext())) {
                (requireActivity() as ChatActivity).showSnackbar(
                    Constants.SNACKBAR_TYPE, getString(
                        R.string.error_server_connection_problem
                    ), MegaApiJava.INVALID_HANDLE
                )
                return
            }
            val cC = ContactController(requireActivity())
            val contactEmails: ArrayList<String>
            val usersCount = message.message?.usersCount ?: 0
            if (usersCount == 1L) {
                cC.inviteContact(message.message?.getUserEmail(0))
            } else {
                Timber.d("Num users to invite: %s", usersCount)
                contactEmails = ArrayList()
                for (j in 0 until usersCount) {
                    message.message?.getUserEmail(j)?.let { userMail ->
                        contactEmails.add(userMail)
                    }
                }
                cC.inviteMultipleContacts(contactEmails)
            }
        } else if (id == R.id.option_start_conversation_layout) {
            val numUsers = message.message?.usersCount ?: 0
            if (numUsers == 1L) {
                message.message?.getUserHandle(0)?.let {
                    (requireActivity() as ChatActivity).startConversation(it)
                }
            } else {
                Timber.d("Num users to invite: %s", numUsers)
                val contactHandles = ArrayList<Long>()
                for (j in 0 until numUsers) {
                    message.message?.getUserHandle(j)?.let { userHandle ->
                        contactHandles.add(userHandle)
                    }
                }
                (requireActivity() as ChatActivity).startGroupConversation(contactHandles)
            }
        } else if (id == R.id.option_download_layout) {
            if (node == null) {
                Timber.w("The selected node is NULL")
                return
            }
            val nodeList = message.message?.megaNodeList
            if (nodeList != null && nodeList.size() > 0) {
                (requireActivity() as ChatActivity).downloadNodeList(nodeList)
            }
        } else if (id == R.id.option_import_layout) {
            if (node == null) {
                Timber.w("The selected node is NULL")
                return
            }
            chatC.importNode(messageId, chatId, Constants.IMPORT_ONLY_OPTION)
        } else if (id == R.id.file_properties_switch || id == R.id.option_save_offline_layout) {
            if (node == null) {
                Timber.w("Message or node is NULL")
                return
            } else if (OfflineUtils.availableOffline(requireContext(), node)) {
                val mOffDelete = dbH.findByHandle(node?.handle ?: -1)
                OfflineUtils.removeOffline(mOffDelete, dbH, requireContext())
                Util.showSnackbar(
                    activity, resources.getString(R.string.file_removed_offline)
                )
            } else {
                checkNotificationsPermission(requireActivity())
                val messages = ArrayList<AndroidMegaChatMessage>()
                messages.add(message)
                chatC.saveForOfflineWithAndroidMessages(
                    messages,
                    megaChatApi.getChatRoom(chatId), requireActivity() as ChatActivity
                )
            }
        } else if (id == R.id.delete_layout) {
            (requireActivity() as ChatActivity).showConfirmationDeleteMessages(
                messagesSelected,
                chatRoom
            )
        }
        closeDialog()
    }

    /**
     * Method to get the node related to the message
     */
    private fun getNode(megaChatMessage: MegaChatMessage?) {
        val msgHandle = message?.message?.megaNodeList?.get(0)?.handle
        if (megaChatMessage?.userHandle == megaChatApi.myUserHandle && msgHandle != null) {
            val disposable = getNodeUseCase.get(msgHandle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { result: MegaNode?, throwable: Throwable? ->
                    if (throwable == null) {
                        node = result
                    }
                    if (node == null) {
                        optionOpenWith.visibility = View.GONE
                        optionForward.visibility = View.GONE
                        optionEdit.visibility = View.GONE
                        optionCopy.visibility = View.GONE
                        optionShare.visibility = View.GONE
                        optionViewContacts.visibility = View.GONE
                        optionInfoContacts.visibility = View.GONE
                        optionStartConversation.visibility = View.GONE
                        optionInviteContact.visibility = View.GONE
                        optionImport.visibility = View.GONE
                        optionDownload.visibility = View.GONE
                        optionSaveOffline.visibility = View.GONE
                        offlineSwitch.visibility = View.GONE
                    } else if (!chatC.isInAnonymousMode) {
                        offlineSwitch.isChecked =
                            OfflineUtils.availableOffline(requireContext(), node)
                        optionSaveOffline.visibility = View.VISIBLE
                        offlineSwitch.setOnCheckedChangeListener { v: CompoundButton, _: Boolean ->
                            onClick(
                                v
                            )
                        }
                    }
                    checkSeparatorsVisibility()
                }
            rxSubscriptions.add(disposable)
        } else {
            nodeList = megaChatMessage?.megaNodeList
            if (nodeList == null || nodeList?.size() == 0) {
                Timber.w("NodeList is NULL or empty")
                return
            }
            node = if (handle == MegaApiJava.INVALID_HANDLE) {
                nodeList?.get(0)
            } else {
                getNodeByHandle(handle)
            }
            if (node == null) {
                Timber.w("Node is NULL")
                return
            }
        }
    }

    /**
     * Get MegaNode
     *
     * @param handle The handle of the node
     * @return The MegaNode
     */
    fun getNodeByHandle(handle: Long): MegaNode? {
        nodeList?.let { nodeList ->
            for (i in 0 until nodeList.size()) {
                val node = nodeList[i]
                if (node.handle == handle) {
                    return node
                }
            }
        }
        return null
    }

    /**
     * Method for checking the visibility of option separators
     */
    private fun checkSeparatorsVisibility() {
        forwardSeparator.visibility = if (optionOpenWith.visibility == View.VISIBLE &&
            optionForward.visibility == View.VISIBLE
        ) View.VISIBLE else View.GONE
        editSeparator.visibility = if (optionForward.visibility == View.VISIBLE &&
            optionEdit.visibility == View.VISIBLE
        ) View.VISIBLE else View.GONE
        copySeparator.visibility = if ((optionEdit.visibility == View.VISIBLE ||
                    optionForward.visibility == View.VISIBLE) &&
            optionCopy.visibility == View.VISIBLE
        ) View.VISIBLE else View.GONE
        shareSeparator.visibility = if (optionForward.visibility == View.VISIBLE &&
            optionShare.visibility == View.VISIBLE
        ) View.VISIBLE else View.GONE
        selectSeparator.visibility = if (optionSelect.visibility == View.VISIBLE &&
            (optionForward.visibility == View.VISIBLE ||
                    optionCopy.visibility == View.VISIBLE)
        ) View.VISIBLE else View.GONE
        infoSeparator.visibility =
            if ((optionViewContacts.visibility == View.VISIBLE ||
                        optionInfoContacts.visibility == View.VISIBLE) &&
                optionSelect.visibility == View.VISIBLE
            ) View.VISIBLE else View.GONE
        inviteSeparator.visibility = if ((optionStartConversation.visibility == View.VISIBLE ||
                    optionInviteContact.visibility == View.VISIBLE) &&
            (optionViewContacts.visibility == View.VISIBLE || optionInfoContacts.visibility == View.VISIBLE || selectSeparator.visibility == View.VISIBLE)
        ) View.VISIBLE else View.GONE
        infoFileSeparator.visibility =
            if ((optionImport.visibility == View.VISIBLE || optionDownload.visibility == View.VISIBLE || optionSaveOffline.visibility == View.VISIBLE) &&
                optionSelect.visibility == View.VISIBLE
            ) View.VISIBLE else View.GONE
        deleteSeparator.visibility = optionDelete.visibility
    }

    /**
     * close dialog
     */
    fun closeDialog() {
        setStateBottomSheetBehaviorHidden()
    }

    /**
     * On save instance state event
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(Constants.CHAT_ID, chatId)
        outState.putLong(Constants.MESSAGE_ID, messageId)
        outState.putLong(Constants.POSITION_SELECTED_MESSAGE, positionMessage.toLong())
        outState.putLong(Constants.HANDLE, handle)
        outState.putBoolean(
            Constants.CANNOT_OPEN_FILE_SHOWN,
            isAlertDialogShown(cannotOpenFileDialog)
        )
    }
}