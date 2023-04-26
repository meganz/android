package mega.privacy.android.app.listeners

import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.di.DatabaseEntryPoint
import mega.privacy.android.app.listeners.CreateFolderListener.ExtraAction
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.data.database.DatabaseHandler
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber

/**
 * GetAttrUserListener
 * @property context: Context
 */
class GetAttrUserListener constructor(private val context: Context) : MegaRequestListenerInterface {
    /**
     * Indicates if the request is only to update the DB.
     * If so, the rest of the actions in onRequestFinish() can be ignored.
     */
    private var onlyDBUpdate = false
    private var holderPosition = 0
    private val databaseHandler: DatabaseHandler by lazy { MegaApplication.getInstance().dbH }

    private val databaseEntryPoint: DatabaseEntryPoint by lazy {
        EntryPointAccessors.fromApplication(context.applicationContext)
    }

    /**
     * Constructor to init a request for check the USER_ATTR_MY_CHAT_FILES_FOLDER user's attribute
     * and update the DB with the result.
     *
     * @param context      current application context
     * @param onlyDBUpdate true if the purpose of the request is only update the DB, false otherwise
     */
    constructor(context: Context, onlyDBUpdate: Boolean) : this(context) {
        this.onlyDBUpdate = onlyDBUpdate
    }

    /**
     * Constructor to init a request for check the USER_ATTR_AVATAR user's attribute
     * and updates the holder of the adapter.
     *
     * @param context        current application context
     * @param holderPosition position of the holder to update
     */
    constructor(context: Context, holderPosition: Int) : this(context) {
        this.holderPosition = holderPosition
    }

    /**
     * Callback function for onRequestStart
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {
        // Do nothing
    }

    /**
     * Callback function for onRequestUpdate
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {
        // Do nothing
    }

    /**
     * When calling [MegaApiJava.getUserAttribute], the MegaRequest object won't store
     * node handle, so we can't get user handle from `request.getNodeHandle()`, we should
     * use `api.getContact(request.getEmail())` to get MegaUser, then get user handle from it.
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) =
        with(request) {
            if (type == MegaRequest.TYPE_GET_ATTR_USER) {
                when (paramType) {
                    MegaApiJava.USER_ATTR_MY_CHAT_FILES_FOLDER ->
                        checkMyChatFilesFolderRequest(api, this, e)
                    MegaApiJava.USER_ATTR_FIRSTNAME -> if (e.errorCode == MegaError.API_OK) {
                        databaseEntryPoint.applicationScope().launch {
                            if (!email.isNullOrBlank()) {
                                databaseEntryPoint.localRoomGateway.setContactName(
                                    firstName = text,
                                    mail = email
                                )
                            }
                            api.getContact(email)?.let {
                                ContactUtil.notifyFirstNameUpdate(context, it.handle)
                            }
                        }
                    }
                    MegaApiJava.USER_ATTR_LASTNAME -> if (e.errorCode == MegaError.API_OK) {
                        databaseEntryPoint.applicationScope().launch {
                            if (!email.isNullOrBlank()) {
                                databaseEntryPoint.localRoomGateway.setContactLastName(
                                    lastName = text,
                                    mail = email
                                )
                            }
                            api.getContact(email)?.let {
                                ContactUtil.notifyLastNameUpdate(context, it.handle)
                            }
                        }
                    }
                    MegaApiJava.USER_ATTR_ALIAS -> if (e.errorCode == MegaError.API_OK) {
                        ContactUtil.updateDBNickname(api.contacts, context, megaStringMap)
                    } else {
                        Timber.e("Error recovering the alias %s", e.errorCode)
                    }
                    MegaApiJava.USER_ATTR_AVATAR -> if (e.errorCode == MegaError.API_OK) {
                        (context as? GroupChatInfoActivity)
                            ?.updateParticipantAvatar(holderPosition, email)
                    }
                    MegaApiJava.USER_ATTR_RUBBISH_TIME -> {
                        Intent(BroadcastConstants.ACTION_UPDATE_RB_SCHEDULER).run {
                            if (e.errorCode == MegaError.API_ENOENT) {
                                val daysCount =
                                    if (MegaApplication.getInstance().myAccountInfo.accountType == MegaAccountDetails.ACCOUNT_TYPE_FREE) {
                                        DAYS_USER_FREE
                                    } else {
                                        DAYS_USER_PRO
                                    }
                                putExtra(BroadcastConstants.DAYS_COUNT, daysCount)
                            } else {
                                putExtra(BroadcastConstants.DAYS_COUNT, number)
                            }
                            MegaApplication.getInstance().sendBroadcast(this)
                        }
                    }
                    MegaApiJava.USER_ATTR_RICH_PREVIEWS -> {
                        if (e.errorCode == MegaError.API_ENOENT) {
                            Timber.w("Attribute USER_ATTR_RICH_PREVIEWS not set")
                        }
                        if (numDetails == 1) {
                            MegaApplication.isShowRichLinkWarning = flag
                            MegaApplication.counterNotNowRichLinkWarning = number.toInt()
                        } else if (numDetails == 0) {
                            MegaApplication.isEnabledRichLinks = flag
                            MegaApplication.getInstance()
                                .sendBroadcast(Intent(BroadcastConstants.BROADCAST_ACTION_INTENT_RICH_LINK_SETTING_UPDATE))
                        }
                    }
                }
            }
        }

    /**
     * Callback function for onRequestTemporaryError
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestTemporaryError(api: MegaApiJava?, request: MegaRequest?, e: MegaError?) {
        // Do nothing
    }

    /**
     * Checks if the USER_ATTR_MY_CHAT_FILES_FOLDER user's attribute exists when the request finished:
     * - If not exists but exists a file with the old "My chat files" name, it renames it with the old one.
     * - If the old one neither exists, it launches a request to create it.
     *
     *
     * Updates the DB with the result and, if necessary updates the data in the current Context.
     *
     * @param api     MegaApiJava object
     * @param request result of the request
     * @param e       MegaError received when the request finished
     */
    private fun checkMyChatFilesFolderRequest(
        api: MegaApiJava,
        request: MegaRequest,
        e: MegaError,
    ) {
        var myChatFolderNode: MegaNode? = null
        var myChatFolderFound = false
        when (e.errorCode) {
            MegaError.API_OK -> {
                myChatFolderNode = api.getNodeByHandle(request.nodeHandle)?.apply {
                    api.getNodeByPath(Constants.CHAT_FOLDER, api.rootNode)
                }
            }
            MegaError.API_ENOENT -> {
                api.getNodeByPath(Constants.CHAT_FOLDER, api.rootNode)?.let {
                    if (!api.isInRubbish(it)) {
                        val name = context.getString(R.string.my_chat_files_folder)
                        if (it.name != name) {
                            api.renameNode(
                                it, name, RenameListener(
                                    isMyChatFilesFolder = true,
                                    context = context
                                )
                            )
                        }
                        api.setMyChatFilesFolder(it.handle, SetAttrUserListener(context))
                    }
                }
            }
            else -> {
                Timber.e("Error getting \"My chat files\" folder: %s", e.errorString)
            }
        }
        if (myChatFolderNode != null && !api.isInRubbish(myChatFolderNode)) {
            databaseHandler.myChatFilesFolderHandle = myChatFolderNode.handle
            myChatFolderFound = true
        } else if (!onlyDBUpdate) {
            api.createFolder(
                context.getString(R.string.my_chat_files_folder),
                api.rootNode,
                CreateFolderListener(context, ExtraAction.MY_CHAT_FILES)
            )
        }

        if (onlyDBUpdate) {
            return
        }

        with(context) {
            if (myChatFolderFound) {
                (this as? FileExplorerActivity)?.let {
                    it.setMyChatFilesFolder(myChatFolderNode)
                    it.checkIfFilesExistsInMEGA()
                }

                (this as? ChatActivity)?.let {
                    it.setMyChatFilesFolder(myChatFolderNode)
                    if (it.isForwardingFromNC) {
                        it.handleStoredData()
                    } else {
                        it.proceedWithAction()
                    }
                }

                (this as? NodeAttachmentHistoryActivity)?.let {
                    it.setMyChatFilesFolder(myChatFolderNode)
                    it.handleStoredData()
                }
            }
        }
    }

    companion object {
        private const val DAYS_USER_FREE = 30
        private const val DAYS_USER_PRO = 90
    }
}