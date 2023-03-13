package mega.privacy.android.app.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.palette.graphics.Palette
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication.Companion.getChatManagement
import mega.privacy.android.app.MegaApplication.Companion.getPushNotificationSettingManagement
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.ManageChatHistoryActivity
import mega.privacy.android.app.activities.contract.SelectFileToShareActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToCopyActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToShareActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.AppBarStateChangeListener
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.components.twemoji.EmojiEditText
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FIRST_NAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_LAST_NAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_NICKNAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_PUSH_NOTIFICATION_SETTING
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_RETENTION_TIME
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_DESTROY_ACTION_MODE
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_MANAGE_SHARE
import mega.privacy.android.app.constants.BroadcastConstants.EXTRA_USER_HANDLE
import mega.privacy.android.app.constants.BroadcastConstants.RETENTION_TIME
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HOLD_CHANGE
import mega.privacy.android.app.databinding.ActivityChatContactPropertiesBinding
import mega.privacy.android.app.databinding.LayoutMenuReturnCallBinding
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.listeners.CreateChatListener
import mega.privacy.android.app.listeners.SetAttrUserListener
import mega.privacy.android.app.main.contactSharedFolder.ContactSharedFolderFragment
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.controllers.ContactController
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.listeners.MultipleRequestListener
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity
import mega.privacy.android.app.modalbottomsheet.ContactFileListBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ContactNicknameBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.dialog.AskForDisplayOverActivity
import mega.privacy.android.app.presentation.contact.ContactInfoViewModel
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
import mega.privacy.android.app.presentation.contact.model.ContactInfoState
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.extensions.getQuantityStringOrDefault
import mega.privacy.android.app.presentation.extensions.iconRes
import mega.privacy.android.app.presentation.extensions.isAwayOrOffline
import mega.privacy.android.app.presentation.extensions.isValid
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.app.usecase.CopyNodeUseCase
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase
import mega.privacy.android.app.usecase.data.CopyRequestResult
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.contacts.UserStatus
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaGlobalListenerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Contact info activity
 */
@AndroidEntryPoint
class ContactInfoActivity : BaseActivity(), ActionNodeCallback, MegaRequestListenerInterface {

    /**
     * object handles passcode lock behaviours
     */
    @Inject
    lateinit var passcodeManagement: PasscodeManagement

    /**
     * Use case to subscribe to global events related to MegaChat.
     */
    @Inject
    lateinit var getChatChangesUseCase: GetChatChangesUseCase

    /**
     * Use case for checking name collisions before uploading, copying or moving.
     */
    @Inject
    lateinit var checkNameCollisionUseCase: CheckNameCollisionUseCase

    /**
     * Use case for copying MegaNodes.
     */
    @Inject
    lateinit var copyNodeUseCase: CopyNodeUseCase

    private lateinit var activityChatContactBinding: ActivityChatContactPropertiesBinding
    private val contentContactProperties get() = activityChatContactBinding.contentContactProperties
    private val collapsingAppBar get() = activityChatContactBinding.collapsingAppBar
    private val callInProgress get() = contentContactProperties.callInProgress
    private val viewModel by viewModels<ContactInfoViewModel>()
    private var permissionsDialog: AlertDialog? = null
    private var statusDialog: AlertDialog? = null
    private var setNicknameDialog: AlertDialog? = null

    //Info of the user
    private var startVideo = false
    private var isChatOpen = false
    private var firstLineTextMaxWidthExpanded = 0
    private var firstLineTextMaxWidthCollapsed = 0
    private var contactStateIcon = 0
    private var contactStateIconPaddingLeft = 0
    private var stateToolbar = AppBarStateChangeListener.State.IDLE
    private val megaAttacher = MegaAttacher(this)
    private val nodeSaver = NodeSaver(
        this, this, this,
        showSaveToDeviceConfirmDialog(this)
    )
    private var user: MegaUser? = null
    private var chatHandle: Long = 0
    private var chat: MegaChatRoom? = null
    private var fromContacts = true

    private var drawableShare: Drawable? = null
    private var drawableSend: Drawable? = null
    private var drawableArrow: Drawable? = null
    private var drawableDots: Drawable? = null
    private var shareMenuItem: MenuItem? = null
    private var sendFileMenuItem: MenuItem? = null
    private var isShareFolderExpanded = false
    private var sharedFoldersFragment: ContactSharedFolderFragment? = null
    private var contactInfoActivity: ContactInfoActivity? = null
    private var selectedNode: MegaNode? = null
    private var moveToRubbish = false
    private var parentHandle: Long = 0
    private var bottomSheetDialogFragment: ContactFileListBottomSheetDialogFragment? = null
    private var contactNicknameBottomSheetDialogFragment: ContactNicknameBottomSheetDialogFragment? =
        null
    private lateinit var selectFolderResultLauncher: ActivityResultLauncher<List<MegaUser>>
    private lateinit var selectFileResultLauncher: ActivityResultLauncher<List<MegaUser>>
    private lateinit var selectFolderToCopyLauncher: ActivityResultLauncher<LongArray>
    private val manageShareReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            sharedFoldersFragment?.clearSelections()
            sharedFoldersFragment?.hideMultipleSelect()
            statusDialog?.dismiss()
        }
    }
    private val userNameReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val userHandle = user?.handle
            val extraUserHandle = intent.getLongExtra(EXTRA_USER_HANDLE, MegaApiJava.INVALID_HANDLE)
            if (intent.action == null || userHandle == null || extraUserHandle != userHandle) {
                return
            }
            if (intent.action == ACTION_UPDATE_NICKNAME || intent.action == ACTION_UPDATE_FIRST_NAME || intent.action == ACTION_UPDATE_LAST_NAME) {
                checkNickname(userHandle)
                updateAvatar()
            }
        }
    }
    private val callStatusObserver = Observer { call: MegaChatCall ->
        when (call.status) {
            MegaChatCall.CALL_STATUS_CONNECTING, MegaChatCall.CALL_STATUS_IN_PROGRESS, MegaChatCall.CALL_STATUS_DESTROYED, MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION, MegaChatCall.CALL_STATUS_USER_NO_PRESENT -> {
                checkScreenRotationToShowCall()
                if (call.status == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION &&
                    call.termCode == MegaChatCall.TERM_CODE_TOO_MANY_PARTICIPANTS
                ) {
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getFormattedStringOrDefault(R.string.call_error_too_many_participants),
                        MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                    )
                }
            }
        }
    }

    private val callOnHoldObserver = Observer<MegaChatCall> { checkScreenRotationToShowCall() }
    private val sessionOnHoldObserver =
        Observer<Pair<*, *>> { checkScreenRotationToShowCall() }
    private val chatRoomMuteUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_PUSH_NOTIFICATION_SETTING) {
                ChatUtil.checkSpecificChatNotifications(
                    chatHandle,
                    contentContactProperties.notificationSwitch,
                    contentContactProperties.notificationsMutedText
                )
            }
        }
    }

    private val retentionTimeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_RETENTION_TIME) {
                val seconds = intent.getLongExtra(RETENTION_TIME, Constants.DISABLED_RETENTION_TIME)
                ChatUtil.updateRetentionTimeLayout(
                    contentContactProperties.retentionTimeText,
                    seconds
                )
            }
        }
    }

    private val destroyActionModeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BROADCAST_ACTION_DESTROY_ACTION_MODE) {
                if (sharedFoldersFragment?.isVisible == true) {
                    sharedFoldersFragment?.clearSelections()
                    sharedFoldersFragment?.hideMultipleSelect()
                }
            }
        }
    }

    private val megaGlobalListenerInterface = object : MegaGlobalListenerInterface {
        override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser>?) {
            if (users.isNullOrEmpty()) return

            for (updatedUser in users) {
                if (updatedUser.handle == user?.handle) {
                    user = updatedUser
                    contentContactProperties.emailText.text = updatedUser.email
                    break
                }
            }
        }

        override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert>?) {
            Timber.d("onUserAlertsUpdate")
        }

        override fun onNodesUpdate(api: MegaApiJava, nodeList: ArrayList<MegaNode>?) {
            sharedFoldersFragment?.let {
                if (it.isVisible) {
                    it.setNodes(parentHandle)
                }
            }
            val nodes = megaApi.getInShares(user)
            setFoldersButtonText(nodes)
        }

        override fun onReloadNeeded(api: MegaApiJava) {}
        override fun onAccountUpdate(api: MegaApiJava) {}
        override fun onContactRequestsUpdate(
            api: MegaApiJava,
            requests: ArrayList<MegaContactRequest>?,
        ) {
        }

        override fun onEvent(api: MegaApiJava, event: MegaEvent?) {}
        override fun onSetsUpdate(api: MegaApiJava, sets: ArrayList<MegaSet>?) {}
        override fun onSetElementsUpdate(api: MegaApiJava, elements: ArrayList<MegaSetElement>?) {}
    }

    private val megaChatRequestListenerInterface = object : MegaChatRequestListenerInterface {
        override fun onRequestStart(api: MegaChatApiJava, request: MegaChatRequest) {}
        override fun onRequestUpdate(api: MegaChatApiJava, request: MegaChatRequest) {}
        override fun onRequestFinish(
            api: MegaChatApiJava,
            request: MegaChatRequest,
            e: MegaChatError,
        ) {
            Timber.d("onRequestFinish")
            if (request.type == MegaChatRequest.TYPE_CREATE_CHATROOM) {
                if (e.errorCode == MegaChatError.ERROR_OK) {
                    Timber.d("Chat created ---> open it!")
                    val intent = Intent(this@ContactInfoActivity, ChatActivity::class.java)
                        .setAction(Constants.ACTION_CHAT_SHOW_MESSAGES)
                        .putExtra(Constants.CHAT_ID, request.chatHandle)
                    if (!fromContacts) {
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    this@ContactInfoActivity.startActivity(intent)
                    finish()
                } else {
                    Timber.d("ERROR WHEN CREATING CHAT ${e.errorString}")
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getFormattedStringOrDefault(R.string.create_chat_error),
                        -1
                    )
                }
            }
        }

        override fun onRequestTemporaryError(
            api: MegaChatApiJava,
            request: MegaChatRequest,
            e: MegaChatError,
        ) {
        }
    }

    /**
     * onRequest start callback of @MegaRequestListenerInterface
     */
    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart: ${request.name}")
    }

    /**
     * onRequest start callback of @MegaRequestListenerInterface
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish: ${request.type}, ${request.requestString}")
        if (request.type == MegaRequest.TYPE_GET_ATTR_USER) {
            Timber.d("MegaRequest.TYPE_GET_ATTR_USER")
            if (e.errorCode == MegaError.API_OK) {
                getUserAttributeSuccess(request)
            }
        } else if (request.type == MegaRequest.TYPE_CREATE_FOLDER) {
            createFolderResponse(e)
        } else if (request.type == MegaRequest.TYPE_MOVE) {
            moveRequestResponse(e, api, request)
        } else if (request.type == MegaRequest.TYPE_REMOVE_CONTACT) {
            Timber.d("Contact removed")
            finish()
        }
    }

    private fun moveRequestResponse(
        error: MegaError,
        api: MegaApiJava,
        request: MegaRequest,
    ) {
        try {
            statusDialog?.dismiss()
        } catch (ex: Exception) {
            Timber.d("Status dialogue dismiss exception $ex")
        }
        if (sharedFoldersFragment?.isVisible == true) {
            sharedFoldersFragment?.clearSelections()
            sharedFoldersFragment?.hideMultipleSelect()
        }
        if (error.errorCode == MegaError.API_EOVERQUOTA && api.isForeignNode(request.parentHandle)) {
            showForeignStorageOverQuotaWarningDialog(this@ContactInfoActivity)
        } else if (moveToRubbish) {
            Timber.d("Finish move to Rubbish!")
            val errorText = if (error.errorCode == MegaError.API_OK) {
                getFormattedStringOrDefault(R.string.context_correctly_moved_to_rubbish)
            } else {
                getFormattedStringOrDefault(R.string.context_no_moved)
            }
            showSnackbar(Constants.SNACKBAR_TYPE, errorText, -1)
        } else {
            val errorText = if (error.errorCode == MegaError.API_OK) {
                getFormattedStringOrDefault(R.string.context_correctly_moved)
            } else {
                getFormattedStringOrDefault(R.string.context_no_moved)
            }
            showSnackbar(Constants.SNACKBAR_TYPE, errorText, -1)
        }
        moveToRubbish = false
        Timber.d("Move request finished")
    }

    private fun createFolderResponse(e: MegaError) {
        try {
            statusDialog?.dismiss()
        } catch (ex: Exception) {
            Timber.d("Status dialogue dismiss exception $ex")
        }
        sharedFoldersFragment?.let {
            if (!it.isVisible) return
            val message = if (e.errorCode == MegaError.API_OK) {
                getFormattedStringOrDefault(R.string.context_folder_created)
            } else {
                getFormattedStringOrDefault(R.string.context_folder_no_created)
            }
            showSnackbar(Constants.SNACKBAR_TYPE, message, -1)
            it.setNodes()
        }
    }


    private fun getUserAttributeSuccess(request: MegaRequest) {
        val avatar = buildAvatarFile(this@ContactInfoActivity, "${request.email}.jpg")
        val imBitmap: Bitmap?
        if (avatar?.exists() == true) {
            if (avatar.length() > 0) {
                val bOpts = BitmapFactory.Options()
                imBitmap = BitmapFactory.decodeFile(avatar.absolutePath, bOpts)
                if (imBitmap == null) {
                    avatar.delete()
                } else {
                    collapsingAppBar.toolbarImage.setImageBitmap(imBitmap)
                    if (!imBitmap.isRecycled) {
                        val palette = Palette.from(imBitmap).generate()
                        val swatch = palette.darkVibrantSwatch
                        collapsingAppBar.imageLayout.setBackgroundColor(
                            (swatch ?: return).bodyTextColor
                        )
                    }
                }
            }
        }
    }

    /**
     * onRequestTemporaryError callback of MegaRequestListenerInterface
     */
    override fun onRequestTemporaryError(
        api: MegaApiJava, request: MegaRequest,
        e: MegaError,
    ) {
        Timber.w("onRequestTemporaryError: ${request.name}")
    }

    /**
     * onRequestUpdate callback of MegaRequestListenerInterface
     */
    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    override fun finishRenameActionWithSuccess(newName: String) {
        //No update needed
    }

    override fun actionConfirmed() {
        if (sharedFoldersFragment?.isVisible == true) {
            sharedFoldersFragment?.clearSelections()
            sharedFoldersFragment?.hideMultipleSelect()
        }
    }

    override fun createFolder(folderName: String) {
        //No action needed
    }

    private fun setAppBarOffset(offsetPx: Int = 50) {
        if (callInProgress.callInProgressLayout.isVisible) {
            changeToolbarLayoutElevation()
        } else {
            val params = collapsingAppBar.appBar.layoutParams as CoordinatorLayout.LayoutParams
            val layoutBehaviour = object : CoordinatorLayout.Behavior<AppBarLayout>() {
                override fun onNestedPreScroll(
                    coordinatorLayout: CoordinatorLayout,
                    child: AppBarLayout,
                    target: View,
                    dx: Int,
                    dy: Int,
                    consumed: IntArray,
                    type: Int,
                ) {
                    super.onNestedPreScroll(
                        activityChatContactBinding.fragmentContainer,
                        collapsingAppBar.appBar,
                        target,
                        0,
                        offsetPx,
                        intArrayOf(0, 0),
                        type
                    )
                }

            }
            params.behavior = layoutBehaviour
        }
    }


    /**
     * onCreate life cycle callback of Contact info activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactInfoActivity = this
        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return
        }
        if (savedInstanceState != null) {
            megaAttacher.restoreState(savedInstanceState)
            nodeSaver.restoreState(savedInstanceState)
        }
        configureActivityLaunchers()

        // State icon resource id default value.
        contactStateIcon =
            if (Util.isDarkMode(this)) R.drawable.ic_offline_dark_standard else R.drawable.ic_offline_light
        checkChatChanges()
        megaApi.addGlobalListener(megaGlobalListenerInterface)
        val extras = intent.extras
        if (extras != null) {
            setUpViews()
            getContactData(extras)
            user?.let {
                viewModel.setupData(it.handle, it.email)
            }
            updateUI()
            checkScreenRotationToShowCall()
            updateViewBasedOnNetworkAvailability()
        } else {
            Timber.w("Extras is NULL")
        }
        collectFlows()
        registerBroadcastReceivers()
        registerEventBusObservers()
        startActivity(Intent(this, AskForDisplayOverActivity::class.java))
    }

    private fun configureActivityLaunchers() {
        configureFolderToShareLauncher()
        configureFileToShareLauncher()
        configureFolderToCopyLauncher()
    }

    @SuppressLint("CheckResult")
    private fun configureFolderToCopyLauncher() {
        selectFolderToCopyLauncher =
            registerForActivityResult(SelectFolderToCopyActivityContract()) { result ->
                if (!viewModel.isOnline()) {
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getFormattedStringOrDefault(R.string.error_server_connection_problem),
                        -1
                    )
                } else {
                    statusDialog =
                        createProgressDialog(
                            this,
                            getFormattedStringOrDefault(R.string.context_copying)
                        )
                    val copyHandles = result?.first
                    val toHandle = result?.second
                    if (copyHandles == null || toHandle == null) return@registerForActivityResult
                    checkNameCollisionUseCase.checkHandleList(
                        copyHandles,
                        toHandle,
                        NameCollisionType.COPY
                    )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { (collisions, handlesWithoutCollision): Pair<ArrayList<NameCollision>, LongArray>, throwable: Throwable? ->
                            if (throwable == null) {
                                if (collisions.isNotEmpty()) {
                                    dismissAlertDialogIfExists(statusDialog)
                                    nameCollisionActivityContract?.launch(collisions)
                                }
                                if (handlesWithoutCollision.isNotEmpty()) {
                                    copyNodeUseCase.copy(handlesWithoutCollision, toHandle)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe { copyResult: CopyRequestResult, copyThrowable: Throwable? ->
                                            dismissAlertDialogIfExists(statusDialog)
                                            if (sharedFoldersFragment?.isVisible == true) {
                                                sharedFoldersFragment?.clearSelections()
                                                sharedFoldersFragment?.hideMultipleSelect()
                                            }
                                            copyThrowable?.let { manageCopyMoveException(it) }
                                                ?: showSnackbar(
                                                    Constants.SNACKBAR_TYPE,
                                                    copyResult.getResultText(),
                                                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                                                )
                                        }
                                }
                            }
                        }
                }
            }
    }

    private fun configureFileToShareLauncher() {
        selectFileResultLauncher =
            registerForActivityResult(SelectFileToShareActivityContract()) { result ->
                user?.let {
                    megaAttacher.handleSelectFileResult(result, it, this)
                }
            }
    }

    private fun configureFolderToShareLauncher() {
        selectFolderResultLauncher =
            registerForActivityResult(SelectFolderToShareActivityContract()) { result ->
                if (result == null) return@registerForActivityResult
                if (viewModel.isOnline()) {
                    val selectedContacts =
                        result.getStringArrayListExtra(Constants.SELECTED_CONTACTS)
                    val folderHandle =
                        result.getLongExtra(FileExplorerActivity.EXTRA_SELECTED_FOLDER, 0)
                    val parent = megaApi.getNodeByHandle(folderHandle)
                    if (parent?.isFolder == true) {
                        val dialogBuilder = MaterialAlertDialogBuilder(this)
                        dialogBuilder.setTitle(getFormattedStringOrDefault(R.string.file_properties_shared_folder_permissions))
                        val items = arrayOf<CharSequence>(
                            getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_only),
                            getFormattedStringOrDefault(
                                R.string.file_properties_shared_folder_read_write
                            ),
                            getFormattedStringOrDefault(R.string.file_properties_shared_folder_full_access)
                        )
                        dialogBuilder.setSingleChoiceItems(items, -1) { _, item ->
                            statusDialog = createProgressDialog(
                                this, getFormattedStringOrDefault(
                                    R.string.context_sharing_folder
                                )
                            )
                            permissionsDialog?.dismiss()
                            NodeController(this).shareFolder(parent, selectedContacts, item)
                        }
                        permissionsDialog = dialogBuilder.create()
                        permissionsDialog?.show()
                    }
                } else {
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getFormattedStringOrDefault(R.string.error_server_connection_problem),
                        -1
                    )
                }
            }
    }

    private fun registerEventBusObservers() {
        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observe(this, callStatusObserver)
        LiveEventBus.get(EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall::class.java)
            .observe(this, callOnHoldObserver)
        LiveEventBus.get(EVENT_SESSION_ON_HOLD_CHANGE, Pair::class.java)
            .observe(this, sessionOnHoldObserver)
    }

    private fun registerBroadcastReceivers() {
        registerReceiver(
            manageShareReceiver,
            IntentFilter(BROADCAST_ACTION_INTENT_MANAGE_SHARE)
        )
        registerReceiver(
            chatRoomMuteUpdateReceiver,
            IntentFilter(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING)
        )
        registerReceiver(
            retentionTimeReceiver,
            IntentFilter(ACTION_UPDATE_RETENTION_TIME)
        )
        val userNameUpdateFilter =
            IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE).apply {
                addAction(ACTION_UPDATE_NICKNAME)
                addAction(ACTION_UPDATE_FIRST_NAME)
                addAction(ACTION_UPDATE_LAST_NAME)
            }
        registerReceiver(userNameReceiver, userNameUpdateFilter)
        registerReceiver(
            destroyActionModeReceiver,
            IntentFilter(BROADCAST_ACTION_DESTROY_ACTION_MODE)
        )
    }

    private fun getContactData(extras: Bundle) {
        chatHandle = extras.getLong(Constants.HANDLE, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
        isChatOpen = extras.getBoolean(Constants.ACTION_CHAT_OPEN, false)
        val userEmailExtra = extras.getString(Constants.NAME)
        if (chatHandle != -1L) {
            Timber.d("From chat!!")
            fromContacts = false
            chat = megaChatApi.getChatRoom(chatHandle)
            val userHandle = (chat ?: return).getPeerHandle(0)
            val userHandleEncoded = MegaApiAndroid.userHandleToBase64(userHandle)
            user = megaApi.getContact(userHandleEncoded)
            user?.let {
                checkNickname(it.handle)
            } ?: run {
                var fullName: String? = ""
                val title = ChatUtil.getTitleChat(chat)
                if (!title.isNullOrEmpty()) {
                    fullName = title
                } else if (userEmailExtra != null) {
                    fullName = userEmailExtra
                }
                withoutNickname(fullName)
            }
        } else {
            Timber.d("From contacts!!")
            fromContacts = true
            user = megaApi.getContact(userEmailExtra)
            chat = user?.let {
                checkNickname(it.handle)
                megaChatApi.getChatRoomByUser(it.handle)
            } ?: run {
                withoutNickname(userEmailExtra)
                null
            }
        }

    }

    private fun setUpViews() {
        activityChatContactBinding = ActivityChatContactPropertiesBinding.inflate(layoutInflater)
        setContentView(activityChatContactBinding.root)
        setSupportActionBar(collapsingAppBar.toolbar)
        title = null
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        val isPortrait = Util.isScreenInPortrait(this)
        val width = Util.dp2px(
            if (isPortrait) Constants.MAX_WIDTH_APPBAR_PORT else Constants.MAX_WIDTH_APPBAR_LAND,
            outMetrics
        )
        firstLineTextMaxWidthExpanded = outMetrics.widthPixels - Util.dp2px(108f, outMetrics)
        firstLineTextMaxWidthCollapsed = width
        contactStateIconPaddingLeft = Util.dp2px(8f, outMetrics)
        with(collapsingAppBar) {
            firstLineToolbar.setMaxWidthEmojis(firstLineTextMaxWidthExpanded)
            secondLineToolbar.setPadding(0, 0, 0, if (isPortrait) 11 else 5)
            secondLineToolbar.maxWidth = width
            appBar.post { Runnable { setAppBarOffset() } }
        }
        with(contentContactProperties) {
            nameText.setMaxWidthEmojis(width)
            notificationsLayout.isVisible = true
            notificationsMutedText.isVisible = false
            notificationSwitch.isClickable = false
            retentionTimeText.isVisible = false
            notificationSwitchLayout.setOnClickListener { chatNotificationsClicked() }
            verifyCredentialsLayout.setOnClickListener { verifyCredentialsClicked() }
            sharedFoldersLayout.setOnClickListener { sharedFolderClicked() }
            shareFoldersButton.setOnClickListener { sharedFolderClicked() }
            shareContactLayout.setOnClickListener { shareContactClicked() }
            chatFilesSharedLayout.setOnClickListener { sharedFilesClicked() }
            contactPropertiesLayout.setOnClickListener { contactPropertiesClicked() }
            removeContactLayout.setOnClickListener { showConfirmationRemoveContact() }
            chatVideoCallLayout.setOnClickListener { startingACall(withVideo = true) }
            chatAudioCallLayout.setOnClickListener { startingACall(withVideo = false) }
            sendChatMessageLayout.setOnClickListener { sendMessageToChat() }
            nicknameText.setOnClickListener { modifyNickName() }
        }
        with(callInProgress) {
            callInProgressLayout.isVisible = false
            callInProgressLayout.setOnClickListener {
                CallUtil.returnActiveCall(
                    this@ContactInfoActivity,
                    passcodeManagement
                )
            }
        }
    }

    private fun modifyNickName() {
        if (contentContactProperties.nicknameText.text == getFormattedStringOrDefault(R.string.add_nickname)) {
            showConfirmationSetNickname(null)
        } else if (user != null && !contactNicknameBottomSheetDialogFragment.isBottomSheetDialogShown()) {
            contactNicknameBottomSheetDialogFragment =
                ContactNicknameBottomSheetDialogFragment().apply {
                    show(
                        supportFragmentManager,
                        contactNicknameBottomSheetDialogFragment?.tag
                    )
                }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, data)) {
            return
        }
    }

    private fun updateViewBasedOnNetworkAvailability() {
        if (viewModel.isOnline()) {
            Timber.d("online -- network connection")
            setAvatar()
            with(contentContactProperties) {
                user?.let {
                    val nodes = megaApi.getInShares(it)
                    setFoldersButtonText(nodes)
                    emailText.text = user?.email
                    updateShareContactLayoutVisibility(shouldShow = true)
                    updateSharedFolderLayoutVisibility(shouldShow = true)
                    updateChatOptionsLayoutVisibility(shouldShow = true)
                } ?: run {
                    if (chat != null) {
                        emailText.text = user?.email
                    }
                    updateShareContactLayoutVisibility(shouldShow = false)
                    updateSharedFolderLayoutVisibility(shouldShow = false)
                    updateChatOptionsLayoutVisibility(shouldShow = false)
                }
            }

        } else {
            Timber.d("OFFLINE -- NO network connection")
            with(contentContactProperties) {
                setOfflineAvatar()
                emailText.text = user?.email
                updateShareContactLayoutVisibility(shouldShow = false)
                updateSharedFolderLayoutVisibility(shouldShow = false)
                updateChatOptionsLayoutVisibility(shouldShow = true)
            }
        }
    }

    private fun makeNotificationLayoutVisible() {
        with(contentContactProperties) {
            notificationsLayout.isVisible = true
            dividerNotificationsLayout.isVisible = true
        }
    }

    private fun updateSharedFolderLayoutVisibility(shouldShow: Boolean) {
        with(contentContactProperties) {
            sharedFoldersLayout.isVisible = shouldShow
            dividerSharedFolderLayout.isVisible = shouldShow
        }
    }

    private fun updateShareContactLayoutVisibility(shouldShow: Boolean) {
        with(contentContactProperties) {
            shareContactLayout.isVisible = shouldShow
            dividerShareContactLayout.isVisible = shouldShow
        }
    }

    private fun updateChatOptionsLayoutVisibility(shouldShow: Boolean) {
        with(contentContactProperties) {
            chatOptionsLayout.isVisible = shouldShow
            dividerChatOptionsLayout.isVisible = shouldShow
        }
    }

    private fun updateChatHistoryLayoutVisibility(shouldShow: Boolean) {
        with(contentContactProperties) {
            manageChatHistoryLayout.isVisible = shouldShow
            dividerChatHistoryLayout.isVisible = shouldShow
        }
    }

    private fun updateChatFilesSharedLayoutVisibility(shouldShow: Boolean) {
        with(contentContactProperties) {
            chatFilesShared.isVisible = shouldShow
            dividerChatFilesSharedLayout.isVisible = shouldShow
        }
    }

    private fun getParticipantEmail(): String? {
        return ChatController(this@ContactInfoActivity).getParticipantEmail(
            (chat ?: return null).getPeerHandle(0)
        )
    }

    private fun contactPropertiesClicked() {
        val intentManageChat = Intent(this, ManageChatHistoryActivity::class.java).apply {
            putExtra(Constants.EMAIL, user?.email)
            putExtra(Constants.CHAT_ID, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
            putExtra(Constants.IS_FROM_CONTACTS, fromContacts)
        }
        startActivity(intentManageChat)
    }

    private fun sharedFilesClicked() {
        val nodeHistoryIntent = Intent(this, NodeAttachmentHistoryActivity::class.java)
        val chatId = chat?.chatId
        chatId?.let {
            nodeHistoryIntent.putExtra("chatId", it)
        }
        startActivity(nodeHistoryIntent)
    }

    private fun shareContactClicked() {
        Timber.d("Share contact option")
        if (viewModel.getStorageState() === StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            return
        }
        if (user == null) {
            Timber.d("Selected contact NULL")
            return
        }
        val intent = ChatController.getSelectChatsToAttachContactIntent(this, user)
        shareContactResultLauncher.launch(intent)
    }

    private val shareContactResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            megaAttacher.handleActivityResult(
                Constants.REQUEST_CODE_SELECT_CHAT,
                it.resultCode,
                it.data,
                this
            )
        }

    private fun verifyCredentialsClicked() {
        val intent = Intent(this, AuthenticityCredentialsActivity::class.java)
        intent.putExtra(Constants.EMAIL, (user ?: return).email)
        startActivity(intent)
    }

    /**
     * onSavedInstanceState life cycle callback
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        megaAttacher.saveState(outState)
        nodeSaver.saveState(outState)
    }

    private fun visibilityStateIcon(userStatus: UserStatus) {
        if (stateToolbar == AppBarStateChangeListener.State.EXPANDED && userStatus.isValid()) {
            collapsingAppBar.firstLineToolbar.apply {
                maxLines = 2
                setTrailingIcon(contactStateIcon, contactStateIconPaddingLeft)
                updateMaxWidthAndIconVisibility(firstLineTextMaxWidthExpanded, true)
            }
        } else {
            collapsingAppBar.firstLineToolbar.apply {
                if (stateToolbar == AppBarStateChangeListener.State.EXPANDED) {
                    maxLines = 2
                    updateMaxWidthAndIconVisibility(firstLineTextMaxWidthExpanded, false)
                } else {
                    maxLines = 1
                    updateMaxWidthAndIconVisibility(firstLineTextMaxWidthCollapsed, false)
                }
            }
        }
    }

    private fun checkNickname(contactHandle: Long) {
        val contactDB = ContactUtil.getContactDB(contactHandle) ?: return
        val fullName =
            ContactUtil.buildFullName(contactDB.name, contactDB.lastName, contactDB.mail)
        val nicknameText = contactDB.nickname
        if (nicknameText.isNullOrEmpty()) {
            withoutNickname(fullName)
        } else {
            withNickname(fullName, nicknameText)
        }
    }

    private fun withoutNickname(name: String?) {
        collapsingAppBar.firstLineToolbar.text = name
        contentContactProperties.nameText.isVisible = false
        contentContactProperties.nicknameText.text =
            getFormattedStringOrDefault(R.string.add_nickname)
        setDefaultAvatar()
    }

    private fun withNickname(name: String, nickname: String) {
        collapsingAppBar.firstLineToolbar.text = nickname
        contentContactProperties.nameText.text = name
        contentContactProperties.nameText.isVisible = true
        contentContactProperties.nicknameText.text =
            getFormattedStringOrDefault(R.string.edit_nickname)
        setDefaultAvatar()
    }

    /**
     * onCreateOptionsMenu lifecycle callback
     * Options menu is created and callback are set
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu")
        getActionBarDrawables()

        // Inflate the menu items for use in the action bar
        menuInflater.inflate(R.menu.contact_properties_action, menu)
        shareMenuItem = menu.findItem(R.id.cab_menu_share_folder)
        sendFileMenuItem = menu.findItem(R.id.cab_menu_send_file)
        val returnCallMenuItem = menu.findItem(R.id.action_return_call)
        val rootViewBinding = LayoutMenuReturnCallBinding.inflate(layoutInflater)
        val layoutCallMenuItem = rootViewBinding.layoutMenuCall
        rootViewBinding.chronoMenu.isVisible = false
        returnCallMenuItem.actionView?.setOnClickListener {
            onOptionsItemSelected(
                returnCallMenuItem
            )
        }
        CallUtil.setCallMenuItem(
            returnCallMenuItem,
            layoutCallMenuItem,
            rootViewBinding.chronoMenu
        )
        sendFileMenuItem?.icon = Util.mutateIconSecondary(
            this,
            R.drawable.ic_send_to_contact,
            R.color.white
        )
        if (viewModel.isOnline()) {
            sendFileMenuItem?.isVisible = fromContacts
        } else {
            Timber.d("Hide all - no network connection")
            shareMenuItem?.isVisible = false
            sendFileMenuItem?.isVisible = false
        }
        collapsingAppBar.apply {
            val statusBarColor = getColorForElevation(
                this@ContactInfoActivity,
                resources.getDimension(R.dimen.toolbar_elevation)
            )
            if (Util.isDarkMode(this@ContactInfoActivity)) {
                collapseToolbar.setContentScrimColor(statusBarColor)
            }
            collapseToolbar.setStatusBarScrimColor(statusBarColor)
            appBar.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
                override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
                    stateToolbar = state
                    if (stateToolbar == State.EXPANDED) {
                        firstLineToolbar.setTextColor(
                            ContextCompat.getColor(
                                this@ContactInfoActivity,
                                R.color.white_alpha_087
                            )
                        )
                        secondLineToolbar.setTextColor(
                            ContextCompat.getColor(
                                this@ContactInfoActivity,
                                R.color.white_alpha_087
                            )
                        )
                        setColorFilter(isDark = false)
                        visibilityStateIcon(viewModel.state.value.userStatus)
                    } else if (stateToolbar == State.COLLAPSED) {
                        firstLineToolbar.setTextColor(
                            ContextCompat.getColor(
                                this@ContactInfoActivity,
                                R.color.grey_087_white_087
                            )
                        )
                        secondLineToolbar.setTextColor(
                            getThemeColor(
                                this@ContactInfoActivity,
                                android.R.attr.textColorSecondary
                            )
                        )
                        setColorFilter(isDark = true)
                        visibilityStateIcon(viewModel.state.value.userStatus)
                    }
                }
            })
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun getActionBarDrawables() {
        drawableDots = ContextCompat.getDrawable(this, R.drawable.ic_dots_vertical_white)
            ?.mutate()
        drawableArrow = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_white)?.mutate()
        drawableShare = ContextCompat.getDrawable(this, R.drawable.ic_share)?.mutate()
        drawableSend = ContextCompat.getDrawable(this, R.drawable.ic_send_to_contact)?.mutate()
    }

    private fun setColorFilter(isDark: Boolean) {
        val color = ContextCompat.getColor(
            this,
            if (isDark) R.color.grey_087_white_087 else R.color.white_alpha_087
        )
        val colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            color,
            BlendModeCompat.SRC_IN
        )
        drawableArrow?.colorFilter = colorFilter
        supportActionBar?.setHomeAsUpIndicator(drawableArrow)
        drawableDots?.colorFilter = colorFilter
        collapsingAppBar.toolbar.overflowIcon = drawableDots
        drawableShare?.colorFilter = colorFilter
        shareMenuItem?.icon = drawableShare
        drawableSend?.colorFilter = colorFilter
        sendFileMenuItem?.icon = drawableSend
    }

    /**
     * Handle clicks from menu items
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.cab_menu_share_folder -> pickFolderToShare()
            R.id.cab_menu_send_file -> {
                if (!viewModel.isOnline()) {
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getFormattedStringOrDefault(R.string.error_server_connection_problem),
                        -1
                    )
                    return true
                }
                sendFileToChat()
            }
            R.id.action_return_call -> {
                CallUtil.returnActiveCall(this, passcodeManagement)
                return true
            }
        }
        return true
    }

    private fun sendFileToChat() {
        Timber.d("sendFileToChat")
        if (viewModel.getStorageState() === StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            return
        }
        user?.let { selectFileResultLauncher.launch(arrayListOf(it)) }
            ?: run { Timber.w("Selected contact NULL") }
    }


    private fun sendMessageToChat() {
        Timber.d("sendMessageToChat")
        if (!CallUtil.checkConnection(this)) return
        if (viewModel.getStorageState() === StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            return
        }
        user?.let {
            val chat = megaChatApi.getChatRoomByUser(it.handle)
            if (chat == null) {
                Timber.d("No chat, create it!")
                val peers = MegaChatPeerList.createInstance()
                peers.addPeer(it.handle, MegaChatPeerList.PRIV_STANDARD)
                megaChatApi.createChat(false, peers, megaChatRequestListenerInterface)
            } else {
                Timber.d("There is already a chat, open it!")
                if (fromContacts) {
                    val intentOpenChat = Intent(this, ChatActivity::class.java)
                    intentOpenChat.action = Constants.ACTION_CHAT_SHOW_MESSAGES
                    intentOpenChat.putExtra(Constants.CHAT_ID, chat.chatId)
                    this.startActivity(intentOpenChat)
                } else {
                    val intentOpenChat = Intent(this, ChatActivity::class.java)
                    intentOpenChat.action = Constants.ACTION_CHAT_SHOW_MESSAGES
                    intentOpenChat.putExtra(Constants.CHAT_ID, chat.chatId)
                    intentOpenChat.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    this.startActivity(intentOpenChat)
                }
                finish()
            }
        }
    }

    /**
     * Start call with chat created
     *
     * @param chatId Chat id.
     */
    private fun startCallWithChat(chatId: Long) {
        if (chatId != MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            enableCallLayouts(false)
            val audio = hasPermissions(this, Manifest.permission.RECORD_AUDIO)
            var video = startVideo
            if (video) {
                video = hasPermissions(this, Manifest.permission.RECORD_AUDIO)
            }
            viewModel.onCallTap(chatId, video, audio)
        }
    }

    /**
     * Start call
     */
    private fun startCall() {
        user?.let {
            viewModel.getChatRoomId(it.handle).observe(this) { chatId -> startCallWithChat(chatId) }
        }
    }

    /**
     * Collecting Flows from ViewModel
     */
    private fun collectFlows() {
        this.collectFlow(
            viewModel.state,
            Lifecycle.State.STARTED
        ) { contactInfoState: ContactInfoState ->
            if (contactInfoState.error != null) {
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getFormattedStringOrDefault(R.string.call_error),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
            } else if (contactInfoState.isCallStarted == true) {
                enableCallLayouts(true)
            }
            updateVerifyCredentialsLayout(contactInfoState.areCredentialsVerified)
            updateUserStatusChanges(contactInfoState)
        }
    }

    private fun updateUserStatusChanges(contactInfoState: ContactInfoState) {
        contactStateIcon =
            contactInfoState.userStatus.iconRes(isLightTheme = !Util.isDarkMode(this))
        collapsingAppBar.secondLineToolbar.apply {
            if (contactInfoState.userStatus.isValid()) {
                isVisible = true
                text = getFormattedStringOrDefault(contactInfoState.userStatus.text)
            } else isVisible = false
        }
        visibilityStateIcon(contactInfoState.userStatus)
        if (contactInfoState.userStatus.isAwayOrOffline()) {
            val formattedDate = TimeUtils.lastGreenDate(this, contactInfoState.lastGreen)
            collapsingAppBar.secondLineToolbar.apply {
                isVisible = true
                text = formattedDate
                isMarqueeIsNecessary(this@ContactInfoActivity)
            }
        }
    }

    /**
     * callback when permission alert is shown to the user
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty()) return
        when (requestCode) {
            Constants.REQUEST_RECORD_AUDIO -> if (CallUtil.checkCameraPermission(this)) {
                startCall()
            }
            Constants.REQUEST_CAMERA -> startCall()
        }
        nodeSaver.handleRequestPermissionsResult(requestCode)
    }

    private fun pickFolderToShare() {
        Timber.d("pickFolderToShare")
        user?.let {
            selectFolderResultLauncher.launch(arrayListOf(it))
        } ?: run {
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                getFormattedStringOrDefault(R.string.error_sharing_folder),
                -1
            )
            Timber.w("Error sharing folder")
        }
    }

    private fun setAvatar() {
        Timber.d("setAvatar")
        user?.let {
            val avatar = buildAvatarFile(this, "${it.email}.jpg")
            if (avatar?.exists() == true) {
                setProfileAvatar(avatar)
            }
        }
    }

    private fun setOfflineAvatar() {
        val userEmail = getParticipantEmail() ?: return
        Timber.d("setOfflineAvatar")
        val avatar = buildAvatarFile(this, "$userEmail.jpg")
        if (avatar?.exists() == true) {
            val imBitmap: Bitmap?
            if (avatar.length() > 0) {
                val bOpts = BitmapFactory.Options()
                imBitmap = BitmapFactory.decodeFile(avatar.absolutePath, bOpts)
                imBitmap?.let {
                    collapsingAppBar.toolbarImage.setImageBitmap(it)
                    if (!it.isRecycled) {
                        val colorBackground = AvatarUtil.getDominantColor(it)
                        collapsingAppBar.imageLayout.setBackgroundColor(colorBackground)
                    }
                }
            }
        }
    }

    private fun setProfileAvatar(avatar: File) {
        Timber.d("setProfileAvatar")
        val imBitmap: Bitmap?
        if (avatar.length() > 0) {
            val bOpts = BitmapFactory.Options()
            imBitmap = BitmapFactory.decodeFile(avatar.absolutePath, bOpts)
            if (imBitmap == null) {
                avatar.delete()
                user?.let {
                    val avatarFile = buildAvatarFile(this, it.email)
                    if (avatarFile?.exists() == true) {
                        megaApi.getUserAvatar(user, avatarFile.absolutePath, this)
                    }
                }

            } else {
                collapsingAppBar.toolbarImage.setImageBitmap(imBitmap)
                if (!imBitmap.isRecycled) {
                    val colorBackground = AvatarUtil.getDominantColor(imBitmap)
                    collapsingAppBar.imageLayout.setBackgroundColor(colorBackground)
                }
            }
        }
    }

    private fun setDefaultAvatar() {
        Timber.d("setDefaultAvatar")
        val defaultAvatar = Bitmap.createBitmap(
            outMetrics.widthPixels, outMetrics.widthPixels, Bitmap.Config.ARGB_8888
        )
        val c = Canvas(defaultAvatar)
        val p = Paint()
        p.isAntiAlias = true
        p.color = Color.TRANSPARENT
        c.drawPaint(p)
        collapsingAppBar.apply {
            imageLayout.setBackgroundColor(AvatarUtil.getColorAvatar(user))
            toolbarImage.setImageBitmap(defaultAvatar)
        }
    }

    private fun startingACall(withVideo: Boolean) {
        startVideo = withVideo
        if (CallUtil.canCallBeStartedFromContactOption(this, passcodeManagement)) {
            startCall()
        }
    }

    /**
     * Shows confirmation message for nick name
     */
    fun showConfirmationSetNickname(alias: String?) {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(
            Util.scaleWidthPx(20, outMetrics),
            Util.scaleHeightPx(16, outMetrics),
            Util.scaleWidthPx(17, outMetrics),
            0
        )
        val emojiEditText = EmojiEditText(this).apply {
            layout.addView(this, params)
            setSingleLine()
            setSelectAllOnFocus(true)
            requestFocus()
            setTextColor(getThemeColor(this@ContactInfoActivity, android.R.attr.textColorSecondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setEmojiSize(Util.dp2px(Constants.EMOJI_SIZE.toFloat(), outMetrics))
            imeOptions = EditorInfo.IME_ACTION_DONE
            inputType = InputType.TYPE_CLASS_TEXT
            Util.showKeyboardDelayed(this)
            setImeActionLabel(
                getFormattedStringOrDefault(R.string.add_nickname),
                EditorInfo.IME_ACTION_DONE
            )
            hint = alias ?: getFormattedStringOrDefault(R.string.nickname_title)
            alias?.let {
                setText(alias)
                setSelection(this.length())
            }
        }

        val colorDisableButton = ContextCompat.getColor(this, R.color.teal_300_038_teal_200_038)
        val colorEnableButton = ContextCompat.getColor(this, R.color.teal_300_teal_200)

        emojiEditText.doAfterTextChanged { text ->
            setNicknameDialog?.let {
                val okButton = it.getButton(AlertDialog.BUTTON_POSITIVE)
                val shouldEnable = text?.isNotEmpty() ?: false
                okButton.apply {
                    isEnabled = shouldEnable
                    setTextColor(if (shouldEnable) colorEnableButton else colorDisableButton)
                }
            }
        }

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(
                if (alias == null) getFormattedStringOrDefault(R.string.add_nickname) else getFormattedStringOrDefault(
                    R.string.edit_nickname
                )
            )
            .setPositiveButton(getFormattedStringOrDefault(R.string.button_set)) { _, _ ->
                val name = emojiEditText.text.toString()
                if (TextUtil.isTextEmpty(name)) {
                    Timber.w("Input is empty")
                    emojiEditText.error = getFormattedStringOrDefault(R.string.invalid_string)
                    emojiEditText.requestFocus()
                } else {
                    addNickname(alias, name)
                    setNicknameDialog?.dismiss()
                }
            }
        builder.setNegativeButton(getFormattedStringOrDefault(R.string.general_cancel)) { _, _ -> setNicknameDialog?.dismiss() }
        builder.setView(layout)
        setNicknameDialog = builder.create().apply {
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).apply {
                isEnabled = false
                setTextColor(colorDisableButton)
            }
        }
    }

    /**
     * Method responsible for adding nick name
     */
    fun addNickname(oldNickname: String?, newNickname: String?) {
        if (oldNickname != null && oldNickname == newNickname) return
        //Update the new nickname
        megaApi.setUserAlias((user ?: return).handle, newNickname, SetAttrUserListener(this))
    }

    private fun updateAvatar() {
        if (viewModel.isOnline()) {
            setAvatar()
        } else if (chat != null) {
            setOfflineAvatar()
        }
    }

    @Suppress("DEPRECATION")
    private fun showConfirmationRemoveContact() {
        Timber.d("showConfirmationRemoveContact")
        MaterialAlertDialogBuilder(this)
            .setTitle(
                getQuantityStringOrDefault(
                    R.plurals.title_confirmation_remove_contact,
                    1
                )
            )
            .setMessage(getQuantityStringOrDefault(R.plurals.confirmation_remove_contact, 1))
            .setPositiveButton(R.string.general_remove) { _, _ ->
                ContactController(this@ContactInfoActivity).removeContact(user)
            }
            .setNegativeButton(R.string.general_cancel) { _, _ -> }
            .show()
    }


    /**
     * onDestroy life cycle call back
     * Broadcast receivers are unregistered
     */
    override fun onDestroy() {
        super.onDestroy()
        drawableArrow?.colorFilter = null
        drawableDots?.colorFilter = null
        drawableSend?.colorFilter = null
        drawableShare?.colorFilter = null
        megaApi.removeGlobalListener(megaGlobalListenerInterface)
        megaApi.removeRequestListener(this)
        megaChatApi.removeChatRequestListener(megaChatRequestListenerInterface)
        unregisterReceiver(chatRoomMuteUpdateReceiver)
        unregisterReceiver(retentionTimeReceiver)
        unregisterReceiver(manageShareReceiver)
        unregisterReceiver(userNameReceiver)
        unregisterReceiver(destroyActionModeReceiver)
        nodeSaver.destroy()
    }

    /**
     * onResume life cycle callback
     */
    override fun onResume() {
        super.onResume()
        checkScreenRotationToShowCall()
        user?.let {
            viewModel.getUserStatusAndRequestForLastGreen(it.handle)
        }
    }

    private fun sharedFolderClicked() {
        val sharedFolderLayout =
            findViewById<View>(R.id.shared_folder_list_container) as RelativeLayout
        if (isShareFolderExpanded) {
            sharedFolderLayout.visibility = View.GONE
            user?.let {
                setFoldersButtonText(megaApi.getInShares(it))
            }
        } else {
            sharedFolderLayout.visibility = View.VISIBLE
            contentContactProperties.shareFoldersButton.setText(R.string.general_close)
            if (sharedFoldersFragment == null) {
                supportFragmentManager.commitNow {
                    sharedFoldersFragment = ContactSharedFolderFragment().apply {
                        setUserEmail(user?.email)
                        replace(
                            R.id.fragment_container_shared_folders,
                            this,
                            "sharedFoldersFragment"
                        )
                    }
                }
            }
        }
        isShareFolderExpanded = !isShareFolderExpanded
    }

    /**
     * Update UI elements if chat exists.
     */
    private fun updateUI() {
        contentContactProperties.apply {
            val chatId = chat?.chatId
            if (chatId == null || chatId == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                updateChatFilesSharedLayoutVisibility(shouldShow = false)
                updateChatHistoryLayoutVisibility(shouldShow = false)
                retentionTimeText.isVisible = false
            } else {
                chatHandle = chatId
                if (!isChatOpen) {
                    getChatManagement().openChatRoom(chatId)
                }
                ChatUtil.updateRetentionTimeLayout(
                    retentionTimeText,
                    ChatUtil.getUpdatedRetentionTimeFromAChat(chatHandle)
                )
                if (viewModel.isOnline()) {
                    updateChatHistoryLayoutVisibility(shouldShow = true)
                } else {
                    updateChatHistoryLayoutVisibility(shouldShow = false)
                }
                updateChatFilesSharedLayoutVisibility(shouldShow = true)
            }
            ChatUtil.checkSpecificChatNotifications(
                chatHandle,
                notificationSwitch,
                notificationsMutedText
            )
            makeNotificationLayoutVisible()
        }
    }

    /**
     * Method that makes the necessary updates when the chat has been created.
     *
     * @param newChats The created chats.
     */
    private fun chatsCreated(newChats: List<MegaChatRoom>) {
        if (newChats.isNotEmpty()) {
            val newChat = newChats[0]
            if (newChat.chatId != MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                chat = newChat
                updateUI()
                chatNotificationsClicked()
            }
        }
    }

    /**
     * Make the necessary actions when clicking on the Chat Notifications layout.
     */
    private fun chatNotificationsClicked() {
        if (chatHandle == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            Timber.d("The chat doesn't exist, create it")
            val chats = ArrayList<MegaChatRoom>()
            val usersNoChat = ArrayList<MegaUser>()
            user?.let {
                usersNoChat.add(it)
                val listener = CreateChatListener(
                    CreateChatListener.CONFIGURE_DND, chats, usersNoChat, this, this
                ) { newChats: List<MegaChatRoom> -> chatsCreated(newChats) }
                val peers = MegaChatPeerList.createInstance()
                peers.addPeer(it.handle, MegaChatPeerList.PRIV_STANDARD)
                megaChatApi.createChat(false, peers, listener)
            }
        } else {
            Timber.d("The chat exists")
            if (contentContactProperties.notificationSwitch.isChecked) {
                ChatUtil.createMuteNotificationsAlertDialogOfAChat(this, chatHandle)
            } else {
                getPushNotificationSettingManagement().controlMuteNotificationsOfAChat(
                    this,
                    Constants.NOTIFICATIONS_ENABLED,
                    chatHandle
                )
            }
        }
    }

    /**
     * Shows contact file list bottom sheet fragment
     */
    fun showOptionsPanel(node: MegaNode?) {
        Timber.d("showOptionsPanel")
        if (node == null || bottomSheetDialogFragment.isBottomSheetDialogShown()) return
        selectedNode = node
        bottomSheetDialogFragment = ContactFileListBottomSheetDialogFragment().apply {
            show(supportFragmentManager, this.tag)
        }
    }

    /**
     * Returns the selected node
     */
    fun getSelectedNode(): MegaNode? = selectedNode

    /**
     * Returns the current set nick name of user
     */
    val nickname: String
        get() = ContactUtil.getNicknameContact(user!!.handle)

    /**
     * Method responsible for downloading file
     */
    fun downloadFile(nodes: List<MegaNode>) {
        checkNotificationsPermission(this)
        nodeSaver.saveNodes(
            nodes,
            highPriority = true,
            isFolderLink = false,
            fromMediaViewer = false,
            needSerialize = false
        )
    }

    /**
     * Helps to move folder
     */
    fun showMove(handleList: ArrayList<Long>) {
        Timber.d("move folder")
        moveToRubbish = false
        val intent = Intent(this, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_MOVE_FOLDER
        val longArray = LongArray(handleList.size)
        for (i in handleList.indices) {
            longArray[i] = handleList[i]
        }
        intent.putExtra("MOVE_FROM", longArray)
        moveFolderIntentResultLauncher.launch(intent)
    }

    private val moveFolderIntentResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            nodeSaver.handleActivityResult(
                this,
                Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE,
                it.resultCode,
                it.data
            )
        }

    /**
     * Method responsible for copying files
     */
    fun showCopy(handleList: ArrayList<Long>) {
        val longArray = LongArray(handleList.size)
        for (i in handleList.indices) {
            longArray[i] = handleList[i]
        }
        selectFolderToCopyLauncher.launch(longArray)
    }

    /**
     * Confirmation alert before moving a file to rubbish bin
     */
    fun askConfirmationMoveToRubbish(handleList: ArrayList<Long>?) {
        Timber.d("askConfirmationMoveToRubbish")
        if (!handleList.isNullOrEmpty()) {
            val message = if (handleList.size > 1) {
                getFormattedStringOrDefault(R.string.confirmation_move_to_rubbish_plural)
            } else {
                getFormattedStringOrDefault(R.string.confirmation_move_to_rubbish)
            }
            MaterialAlertDialogBuilder(this)
                .setMessage(message)
                .setPositiveButton(R.string.general_move) { _, _ ->
                    moveToTrash(handleList)
                }
                .setNegativeButton(R.string.general_cancel) { _, _ -> }
                .show()
        } else {
            Timber.w("handleList NULL")
        }
    }

    /**
     * Returns true when parent stack is empty
     */
    val isEmptyParentHandleStack: Boolean
        get() = sharedFoldersFragment?.isEmptyParentHandleStack() ?: true

    private fun moveToTrash(handleList: ArrayList<Long>?) {
        Timber.d("moveToTrash: ")
        moveToRubbish = true
        if (!viewModel.isOnline()) {
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                getFormattedStringOrDefault(R.string.error_server_connection_problem),
                -1
            )
            return
        }
        val moveMultipleListener: MultipleRequestListener?
        //Check if the node is not yet in the rubbish bin (if so, remove it)
        if (handleList != null) {
            if (handleList.size > 1) {
                Timber.d("MOVE multiple: ${handleList.size}")
                moveMultipleListener =
                    MultipleRequestListener(Constants.MULTIPLE_SEND_RUBBISH, this)
                for (i in handleList.indices) {
                    megaApi.moveNode(
                        megaApi.getNodeByHandle(handleList[i]),
                        megaApi.rubbishNode,
                        moveMultipleListener
                    )
                }
            } else {
                Timber.d("MOVE single")
                megaApi.moveNode(
                    megaApi.getNodeByHandle(handleList[0]),
                    megaApi.rubbishNode,
                    this
                )
            }
        } else {
            Timber.w("handleList NULL")
            return
        }
    }

    /**
     * Sets the parent handle globally
     */
    fun setParentHandle(parentHandle: Long) {
        this.parentHandle = parentHandle
    }

    private fun setFoldersButtonText(nodes: ArrayList<MegaNode>?) {
        if (nodes != null) {
            contentContactProperties.apply {
                shareFoldersButton.text = getQuantityStringOrDefault(
                    R.plurals.num_folders_with_parameter,
                    nodes.size,
                    nodes.size
                )
                if (nodes.size == 0) {
                    shareFoldersButton.isClickable = false
                    sharedFoldersLayout.isClickable = false
                }
            }
        }
    }

    private fun enableCallLayouts(enable: Boolean) {
        contentContactProperties.apply {
            chatVideoCallLayout.isEnabled = enable
            chatAudioCallLayout.isEnabled = enable
        }
    }

    /**
     * Updates the "Verify credentials" view.
     *
     * @param verified True if contact credentials are verified, false otherwise.
     */
    private fun updateVerifyCredentialsLayout(verified: Boolean) {
        contentContactProperties.apply {
            if (user != null) {
                verifyCredentialsLayout.isVisible = true
                if (verified) {
                    verifyCredentialsInfo.setText(R.string.label_verified)
                    verifyCredentialsInfoIcon.isVisible = true
                } else {
                    verifyCredentialsInfo.setText(R.string.label_not_verified)
                    verifyCredentialsInfoIcon.isVisible = false
                }
            } else {
                verifyCredentialsLayout.isVisible = false
            }
        }
    }

    /**
     * This method is used to change the elevation of the toolbar.
     */
    fun changeToolbarLayoutElevation() {
        collapsingAppBar.appBar.elevation =
            if (callInProgress.callInProgressLayout.isVisible) {
                Util.dp2px(16f, outMetrics).toFloat()
            } else {
                0.toFloat()
            }
        if (callInProgress.callInProgressLayout.visibility == View.VISIBLE) {
            collapsingAppBar.appBar.setExpanded(false)
        }
    }

    /**
     * Method to check the rotation of the screen to display the call properly.
     */
    private fun checkScreenRotationToShowCall() {
        if (Util.isScreenInPortrait(this@ContactInfoActivity)) {
            setCallWidget()
        } else {
            this.invalidateOptionsMenu()
        }
    }

    /**
     * This method sets "Tap to return to call" banner when there is a call in progress.
     */
    private fun setCallWidget() {
        callInProgress.apply {
            if (!Util.isScreenInPortrait(this@ContactInfoActivity)) {
                CallUtil.hideCallWidget(
                    this@ContactInfoActivity,
                    callInProgressChrono,
                    callInProgressLayout
                )
                return
            }
            CallUtil.showCallLayout(
                this@ContactInfoActivity,
                callInProgressLayout,
                callInProgressChrono,
                callInProgressText
            )
        }
    }

    /**
     * Method handle snack bar messages
     */
    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, activityChatContactBinding.fragmentContainer, content, chatId)
    }

    /**
     * Receive changes to OnChatOnlineStatusUpdate, OnChatConnectionStateUpdate and OnChatPresenceLastGreen and make the necessary changes
     */
    private fun checkChatChanges() {
        val chatSubscription = getChatChangesUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ next: GetChatChangesUseCase.Result? ->
                if (next is GetChatChangesUseCase.Result.OnChatOnlineStatusUpdate) {
                    user?.let {
                        viewModel.getUserStatusAndRequestForLastGreen(it.handle)
                    }
                }
                if (next is GetChatChangesUseCase.Result.OnChatConnectionStateUpdate) {
                    val chatId = next.chatid
                    val newState = next.newState
                    val chatRoom = megaChatApi.getChatRoom(chatId)
                    if (CallUtil.isChatConnectedInOrderToInitiateACall(newState, chatRoom)) {
                        startCall()
                    }
                }
                if (next is GetChatChangesUseCase.Result.OnChatPresenceLastGreen) {
                    val userHandle = next.userHandle
                    val lastGreen = next.lastGreen
                    viewModel.updateLastGreen(userHandle, lastGreen)
                }
            }) { t: Throwable? -> Timber.e(t) }
        composite.add(chatSubscription)
    }
}