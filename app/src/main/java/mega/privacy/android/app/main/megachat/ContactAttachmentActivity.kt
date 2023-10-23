package mega.privacy.android.app.main.megachat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CREDENTIALS
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FIRST_NAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_LAST_NAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_NICKNAME
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE
import mega.privacy.android.app.constants.BroadcastConstants.EXTRA_USER_HANDLE
import mega.privacy.android.app.databinding.ActivityContactAttachmentChatBinding
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.controllers.ContactController
import mega.privacy.android.app.main.megachat.chatAdapters.MegaContactsAttachedAdapter
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ContactAttachmentBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.model.chat.AndroidMegaChatMessage
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.MegaNavigator
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ContactAttachmentActivity : PasscodeActivity(), MegaRequestListenerInterface,
    MegaChatRequestListenerInterface, View.OnClickListener {

    private lateinit var binding: ActivityContactAttachmentChatBinding

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    @Inject
    lateinit var navigator: MegaNavigator

    @JvmField
    var selectedEmail: String? = null

    private var inviteAction = false
    private lateinit var chatController: ChatController
    private var message: AndroidMegaChatMessage? = null

    @JvmField
    var chatId: Long = 0

    @JvmField
    var messageId: Long = 0
    private var contacts: MutableList<Contact> = mutableListOf()
    private var adapter: MegaContactsAttachedAdapter? = null
    private var bottomSheetDialogFragment: ContactAttachmentBottomSheetDialogFragment? = null
    private val contactUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == null) return
            if (intent.action == ACTION_UPDATE_NICKNAME ||
                intent.action == ACTION_UPDATE_FIRST_NAME ||
                intent.action == ACTION_UPDATE_LAST_NAME ||
                intent.action == ACTION_UPDATE_CREDENTIALS
            ) {
                updateAdapter(intent.getLongExtra(EXTRA_USER_HANDLE, MegaApiJava.INVALID_HANDLE))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityContactAttachmentChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return
        }
        chatController = ChatController(this)
        if (intent != null) {
            chatId = intent.getLongExtra(Constants.CHAT_ID, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
            messageId =
                intent.getLongExtra(Constants.MESSAGE_ID, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
            Timber.d("Chat ID: %d, Message ID: %d", chatId, messageId)
            val messageMega: MegaChatMessage? = megaChatApi.getMessage(chatId, messageId)
            if (messageMega != null) {
                message = AndroidMegaChatMessage(messageMega)
            }
        }
        message?.let { androidMegaChatMessage ->
            androidMegaChatMessage.message?.let { megaChatMessage ->
                for (i in 0 until megaChatMessage.usersCount) {
                    val email: String = megaChatMessage.getUserEmail(i)
                    val contactDB: Contact? = dbH.findContactByEmail(email)
                    if (contactDB != null) {
                        contacts.add(contactDB)
                    } else {
                        val handle: Long = megaChatMessage.getUserHandle(i)
                        val newContactDB = Contact(
                            handle,
                            email,
                            "",
                            megaChatMessage.getUserName(i),
                            ""
                        )
                        contacts.add(newContactDB)
                    }
                }
            } ?: { Timber.w("MegaChat message is null") }
        } ?: { finish() }

        //Set toolbar
        setSupportActionBar(binding.toolbarContactAttachmentChat)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.activity_title_contacts_attached)

            message?.message?.let {
                val title =
                    if (it.userHandle == megaChatApi.myUserHandle)
                        megaChatApi.myFullname
                    else
                        chatController.getParticipantFullName(
                            it.userHandle
                        )
                subtitle = title
            }
        }

        for (contactDB in contacts) {
            if (contactDB.email == null) continue
            val checkContact: MegaUser? = megaApi.getContact(contactDB.email)
            if (contactDB.email != megaApi.myEmail &&
                (checkContact == null || checkContact.visibility != MegaUser.VISIBILITY_VISIBLE)
            ) {
                inviteAction = true
                break
            }
        }
        binding.contactAttachmentChatOptionButton.apply {
            setOnClickListener(this@ContactAttachmentActivity)
            setText(if (inviteAction) R.string.menu_add_contact else R.string.group_chat_start_conversation_label)
        }
        binding.contactAttachmentChatCancelButton.setOnClickListener(this)
        binding.contactAttachmentChatViewBrowser.apply {
            clipToPadding = false
            addItemDecoration(SimpleDividerItemDecoration(this@ContactAttachmentActivity))
        }
        binding.contactAttachmentChatViewBrowser.layoutManager = LinearLayoutManager(this)
        binding.contactAttachmentChatViewBrowser.itemAnimator =
            Util.noChangeRecyclerViewItemAnimator()
        if (adapter == null) {
            adapter = MegaContactsAttachedAdapter(
                this,
                contacts.toList(),
                binding.contactAttachmentChatViewBrowser
            ).apply {
                positionClicked = -1
                isMultipleSelect = false
            }
        }
        binding.contactAttachmentChatViewBrowser.adapter = adapter

        IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE).apply {
            addAction(ACTION_UPDATE_NICKNAME)
            addAction(ACTION_UPDATE_FIRST_NAME)
            addAction(ACTION_UPDATE_LAST_NAME)
            addAction(ACTION_UPDATE_CREDENTIALS)
        }.also {
            registerReceiver(contactUpdateReceiver, it)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        megaApi.removeRequestListener(this)
        unregisterReceiver(contactUpdateReceiver)
    }

    fun showOptionsPanel(email: String?) {
        Timber.d("showOptionsPanel")
        if (email == null || bottomSheetDialogFragment.isBottomSheetDialogShown()) return
        selectedEmail = email
        bottomSheetDialogFragment = ContactAttachmentBottomSheetDialogFragment().also {
            it.show(
                supportFragmentManager,
                it.tag
            )
        }

    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        if (request.type == MegaRequest.TYPE_SHARE) {
            Timber.d("Share")
        }
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish: %d__%s", request.type, request.requestString)
        if (request.type == MegaRequest.TYPE_INVITE_CONTACT) {
            Timber.d("MegaRequest.TYPE_INVITE_CONTACT finished: %s", request.number)
            if (request.number == MegaContactRequest.INVITE_ACTION_REMIND.toLong()) {
                showSnackbar(getString(R.string.context_contact_invitation_resent))
            } else {
                if (e.errorCode == MegaError.API_OK) {
                    Timber.d("OK INVITE CONTACT: %s", request.email)
                    if (request.number == MegaContactRequest.INVITE_ACTION_ADD.toLong()) {
                        showSnackbar(
                            getString(
                                R.string.context_contact_request_sent,
                                request.email
                            )
                        )
                    }
                } else {
                    Timber.e("Code: %s", e.errorString)
                    if (e.errorCode == MegaError.API_EEXIST) {
                        showSnackbar(
                            getString(
                                R.string.context_contact_already_invited,
                                request.email
                            )
                        )
                    } else if (request.number == MegaContactRequest.INVITE_ACTION_ADD.toLong() && e.errorCode == MegaError.API_EARGS) {
                        showSnackbar(getString(R.string.error_own_email_as_contact))
                    } else {
                        showSnackbar(getString(R.string.general_error))
                    }
                    Timber.e("ERROR: %s___%s", e.errorCode, e.errorString)
                }
            }
        }
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava, request: MegaRequest,
        e: MegaError,
    ) {
        Timber.w("onRequestTemporaryError")
    }

    fun itemClick(position: Int) {
        Timber.d("Position: %s", position)

        val c: Contact = contacts[position]
        val contact: MegaUser? = megaApi.getContact(c.email)
        if (contact != null) {
            if (contact.visibility == MegaUser.VISIBILITY_VISIBLE) {
                val chat: MegaChatRoom? = megaChatApi.getChatRoom(chatId)
                val contactHandle: Long = c.userId
                val isChatRoomOpen =
                    chat != null && !chat.isGroup && contactHandle == chat.getPeerHandle(0)
                ContactUtil.openContactInfoActivity(this, c.email, isChatRoomOpen)
            } else {
                Timber.d("The user is not contact")
                showSnackbar(getString(R.string.alert_user_is_not_contact))
            }
        } else {
            Timber.e("The contact is null")
        }
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.contact_attachment_chat_option_button) {
            Timber.d("Click on ACTION button")
            if (inviteAction) {
                val contactEmails = ArrayList<String?>()
                val contactControllerC = ContactController(this)
                contacts.forEach { contact ->
                    val checkContact: MegaUser? = megaApi.getContact(contact.email)
                    if (contact.email != megaApi.myEmail &&
                        (checkContact == null || checkContact.visibility != MegaUser.VISIBILITY_VISIBLE)
                    ) {
                        contactEmails.add(contact.email)
                    }
                }
                if (contactEmails.isNotEmpty()) {
                    contactControllerC.inviteMultipleContacts(contactEmails)
                }
            } else {
                val contactHandles = contacts.map { it.userId }
                startGroupConversation(contactHandles)
            }
        } else if (id == R.id.contact_attachment_chat_cancel_button) {
            Timber.d("Click on Cancel button")
            finish()
        }
    }

    fun setPositionClicked(positionClicked: Int) {
        adapter?.positionClicked = positionClicked
    }

    fun notifyDataSetChanged() {
        adapter?.notifyDataSetChanged()
    }

    fun showSnackbar(s: String) {
        showSnackbar(binding.contactAttachmentChat as View, s)
    }

    fun startConversation(handle: Long) {
        Timber.d("Handle: %s", handle)
        val chat: MegaChatRoom? = megaChatApi.getChatRoomByUser(handle)
        val peers: MegaChatPeerList = MegaChatPeerList.createInstance()
        if (chat == null) {
            Timber.d("No chat, create it!")
            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD)
            megaChatApi.createChat(false, peers, this)
        } else {
            Timber.d("There is already a chat, open it!")
            finish()
            navigator.openChat(
                context = this@ContactAttachmentActivity,
                action = Constants.ACTION_CHAT_SHOW_MESSAGES,
                chatId = chat.chatId
            )
        }
    }

    private fun startGroupConversation(userHandles: List<Long>) {
        val peers: MegaChatPeerList = MegaChatPeerList.createInstance()
        for (handle in userHandles) {
            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD)
        }
        megaChatApi.createChat(true, peers, this)
    }

    override fun onRequestStart(api: MegaChatApiJava, request: MegaChatRequest) {
        Timber.d("onRequestStart: %s", request.requestString)
    }

    override fun onRequestUpdate(api: MegaChatApiJava, request: MegaChatRequest) {}
    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        Timber.d("onRequestFinish: %s", request.requestString)
        if (request.type == MegaChatRequest.TYPE_CREATE_CHATROOM) {
            Timber.d("Create chat request finish!!!")
            if (e.errorCode == MegaChatError.ERROR_OK) {
                Timber.d("Open new chat")
                finish()
                navigator.openChat(
                    context = this@ContactAttachmentActivity,
                    action = Constants.ACTION_CHAT_SHOW_MESSAGES,
                    chatId = request.chatHandle,
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
            } else {
                Timber.e("ERROR WHEN CREATING CHAT %s", e.errorString)
                showSnackbar(getString(R.string.create_chat_error))
            }
        }
    }

    override fun onRequestTemporaryError(
        api: MegaChatApiJava,
        request: MegaChatRequest,
        e: MegaChatError,
    ) {
    }

    private fun updateAdapter(handleReceived: Long) {
        if (contacts.isEmpty()) return

        for (i in contacts.indices) {
            val email = contacts[i].email
            val user = megaApi.getContact(email)
            val handleUser = user?.handle
            if (handleUser == handleReceived) {
                val (userId, _, _, firstName, lastName) = contacts[i]
                adapter?.updateContact(
                    Contact(
                        userId,
                        email,
                        ContactUtil.getNicknameContact(email),
                        firstName,
                        lastName
                    ), i
                )
                break
            }
        }
    }


}