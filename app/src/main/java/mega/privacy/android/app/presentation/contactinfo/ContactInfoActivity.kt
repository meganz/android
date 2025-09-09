package mega.privacy.android.app.presentation.contactinfo

import android.Manifest
import android.app.Activity
import android.content.Intent
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
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.commitNow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getPushNotificationSettingManagement
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.activities.contract.SelectFileToShareActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToCopyActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToShareActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.AppBarStateChangeListener
import mega.privacy.android.app.components.twemoji.EmojiEditText
import mega.privacy.android.app.databinding.ActivityChatContactPropertiesBinding
import mega.privacy.android.app.databinding.LayoutMenuReturnCallBinding
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.showSnackbarWithChat
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.contactSharedFolder.ContactSharedFolderFragment
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity
import mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.modalbottomsheet.ContactFileListBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ContactNicknameBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.OnSharedFolderUpdatedCallBack
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
import mega.privacy.android.app.presentation.contactinfo.model.ContactInfoUiState
import mega.privacy.android.app.presentation.extensions.iconRes
import mega.privacy.android.app.presentation.extensions.isAwayOrOffline
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.extensions.isValid
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.app.presentation.meeting.WaitingRoomManagementViewModel
import mega.privacy.android.app.presentation.meeting.view.dialog.DenyEntryToCallDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.UsersInWaitingRoomDialog
import mega.privacy.android.core.nodecomponents.mapper.message.NodeMoveRequestMessageMapper
import mega.privacy.android.app.presentation.node.dialogs.leaveshare.LeaveShareDialog
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentViewModel
import mega.privacy.android.app.presentation.transfers.attach.createNodeAttachmentView
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel
import mega.privacy.android.app.utils.AlertDialogUtil
import mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.NODE_HANDLES
import mega.privacy.android.app.utils.Constants.SELECTED_CHATS
import mega.privacy.android.app.utils.Constants.SELECTED_USERS
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import javax.inject.Inject

/**
 * Contact info activity
 */
@AndroidEntryPoint
class ContactInfoActivity : BaseActivity(), ActionNodeCallback, MegaRequestListenerInterface,
    OnSharedFolderUpdatedCallBack {

    /**
     * object handles passcode lock behaviours
     */
    @Inject
    lateinit var passcodeCheck: PasscodeCheck

    /**
     * Get theme mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * Move request message mapper
     */
    @Inject
    lateinit var nodeMoveRequestMessageMapper: NodeMoveRequestMessageMapper

    /**
     * Navigator
     */
    @Inject
    lateinit var navigator: MegaNavigator

    private lateinit var activityChatContactBinding: ActivityChatContactPropertiesBinding
    private val contentContactProperties get() = activityChatContactBinding.contentContactProperties
    private val collapsingAppBar get() = activityChatContactBinding.collapsingAppBar

    private val callInProgress get() = contentContactProperties.callInProgress
    private val viewModel by viewModels<ContactInfoViewModel>()
    private val startDownloadViewModel by viewModels<StartDownloadViewModel>()
    private val waitingRoomManagementViewModel by viewModels<WaitingRoomManagementViewModel>()
    private val nodeAttachmentViewModel by viewModels<NodeAttachmentViewModel>()

    private var permissionsDialog: AlertDialog? = null
    private var statusDialog: AlertDialog? = null
    private var setNicknameDialog: AlertDialog? = null

    private var startVideo = false
    private var firstLineTextMaxWidthExpanded = 0
    private var firstLineTextMaxWidthCollapsed = 0
    private var contactStateIcon = 0
    private var contactStateIconPaddingLeft = 0
    private var stateToolbar = AppBarStateChangeListener.State.IDLE
    private var forceAppUpdateDialog: AlertDialog? = null

    private var drawableShare: Drawable? = null
    private var drawableSend: Drawable? = null
    private var drawableArrow: Drawable? = null
    private var drawableDots: Drawable? = null
    private var shareMenuItem: MenuItem? = null
    private var sendFileMenuItem: MenuItem? = null
    private var isShareFolderExpanded = false
    private var sharedFoldersFragment: ContactSharedFolderFragment? = null
    private var selectedNode: MegaNode? = null
    private var moveToRubbish = false
    private var bottomSheetDialogFragment: ContactFileListBottomSheetDialogFragment? = null
    private var contactNicknameBottomSheetDialogFragment: ContactNicknameBottomSheetDialogFragment? =
        null
    private lateinit var selectFolderResultLauncher: ActivityResultLauncher<String>
    private lateinit var selectFileResultLauncher: ActivityResultLauncher<String>
    private lateinit var selectFolderToCopyLauncher: ActivityResultLauncher<LongArray>
    private val nameCollisionActivityLauncher = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        result?.let {
            showSnackbar(SNACKBAR_TYPE, it, INVALID_HANDLE)
        }
    }

    override fun onSharedFolderUpdated() {
        Timber.d("onSharedFolderUpdated")
        hideSelectMode()
        statusDialog?.dismiss()
    }

    override fun showLeaveFolderDialog(nodeIds: List<Long>) {
        viewModel.setLeaveFolderNodeIds(nodeIds)
    }

    private fun navigateToChatActivity(handle: Long) {
        navigator.openChat(
            context = this,
            chatId = handle,
            action = Constants.ACTION_CHAT_SHOW_MESSAGES,
            flags = if (!viewModel.isFromContacts) Intent.FLAG_ACTIVITY_CLEAR_TOP else 0
        )
        finish()
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
        when (request.type) {
            MegaRequest.TYPE_CREATE_FOLDER -> createFolderResponse(e)
            MegaRequest.TYPE_MOVE -> moveRequestResponse(e, api, request)
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
            hideSelectMode()
        }
        if (error.errorCode == MegaError.API_EOVERQUOTA && api.isForeignNode(request.parentHandle)) {
            showForeignStorageOverQuotaWarningDialog(this@ContactInfoActivity)
        } else if (moveToRubbish) {
            Timber.d("Finish move to Rubbish!")
            val errorText = if (error.errorCode == MegaError.API_OK) {
                getString(R.string.context_correctly_moved_to_rubbish)
            } else {
                getString(R.string.context_no_moved)
            }
            showSnackbar(Constants.SNACKBAR_TYPE, errorText, -1)
        } else {
            val errorText = if (error.errorCode == MegaError.API_OK) {
                getString(sharedR.string.context_correctly_moved)
            } else {
                getString(R.string.context_no_moved)
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
                getString(R.string.context_folder_created)
            } else {
                getString(R.string.context_folder_no_created)
            }
            showSnackbar(Constants.SNACKBAR_TYPE, message, -1)
            it.setNodes()
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
        hideSelectMode()
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
        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return
        }
        configureActivityLaunchers()

        // State icon resource id default value.
        contactStateIcon =
            if (Util.isDarkMode(this)) R.drawable.ic_offline_dark_standard else R.drawable.ic_offline_light
        val extras = intent.extras
        if (extras != null) {
            setUpViews()
            getContactData(extras)
            checkScreenRotationToShowCall()
            updateViewBasedOnNetworkAvailability()
        } else {
            Timber.w("Extras is NULL")
        }
        collectFlows()
    }

    private fun configureActivityLaunchers() {
        configureFolderToShareLauncher()
        configureFileToShareLauncher()
        configureFolderToCopyLauncher()
    }

    private fun configureFolderToCopyLauncher() {
        selectFolderToCopyLauncher =
            registerForActivityResult(SelectFolderToCopyActivityContract()) { result ->
                actionConfirmed()
                viewModel.checkCopyNameCollision(handles = result)
            }
    }

    private fun configureFileToShareLauncher() {
        selectFileResultLauncher =
            registerForActivityResult(SelectFileToShareActivityContract()) { result ->
                result?.let {
                    val nodes = it.getLongArrayExtra(NODE_HANDLES)
                    val email = viewModel.userEmail
                    if (nodes != null && nodes.isNotEmpty() && !email.isNullOrEmpty()) {
                        nodeAttachmentViewModel.attachNodesToChatByEmail(
                            nodeIds = nodes.map { handle -> NodeId(handle) },
                            email = email
                        )
                    }
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
                        dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions))
                        val items = arrayOf<CharSequence>(
                            getString(R.string.file_properties_shared_folder_read_only),
                            getString(
                                R.string.file_properties_shared_folder_read_write
                            ),
                            getString(R.string.file_properties_shared_folder_full_access)
                        )
                        dialogBuilder.setSingleChoiceItems(items, -1) { _, item ->
                            statusDialog = createProgressDialog(
                                this, getString(
                                    R.string.context_sharing_folder
                                )
                            )
                            permissionsDialog?.dismiss()
                            lifecycleScope.launch {
                                statusDialog?.show()
                                viewModel.initShareKey(parent)
                                NodeController(this@ContactInfoActivity).shareFolder(
                                    parent,
                                    selectedContacts,
                                    item
                                )
                            }
                        }
                        permissionsDialog = dialogBuilder.create()
                        permissionsDialog?.show()
                    }
                } else {
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.error_server_connection_problem),
                        -1
                    )
                }
            }
    }

    private fun getContactData(extras: Bundle) {
        val chatHandle = extras.getLong(Constants.HANDLE, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
        val userEmailExtra = extras.getString(Constants.NAME)
        viewModel.updateContactInfo(chatHandle, userEmailExtra)
    }

    private fun setUpViews() {
        activityChatContactBinding = ActivityChatContactPropertiesBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(activityChatContactBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }

            collapsingAppBar.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
            }

            WindowInsetsCompat.CONSUMED
        }
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
            notificationSwitchLayout.setOnClickListener { viewModel.chatNotificationsClicked() }
            verifyCredentialsLayout.setOnClickListener { verifyCredentialsClicked() }
            sharedFoldersLayout.setOnClickListener { sharedFolderClicked() }
            shareFoldersButton.setOnClickListener { sharedFolderClicked() }
            shareContactLayout.setOnClickListener { shareContactClicked() }
            chatFilesSharedLayout.setOnClickListener { sharedFilesClicked() }
            contactPropertiesLayout.setOnClickListener { contactPropertiesClicked() }
            removeContactLayout.setOnClickListener { showConfirmationRemoveContact() }
            chatVideoCallLayout.setOnClickListener { startingACall(withVideo = true) }
            chatAudioCallLayout.setOnClickListener { startingACall(withVideo = false) }
            sendChatMessageLayout.setOnClickListener { viewModel.sendMessageToChat() }
            nicknameText.setOnClickListener { modifyNickName() }
        }
        with(callInProgress) {
            callInProgressLayout.isVisible = false
            callInProgressLayout.setOnClickListener {
                CallUtil.returnActiveCall(
                    this@ContactInfoActivity,
                )
            }
        }

        activityChatContactBinding.composeContainer.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                OriginalTheme(isDark = isDark) {
                    UsersInWaitingRoomDialog()
                    DenyEntryToCallDialog()
                    state.leaveFolderNodeIds?.let {
                        LeaveShareDialog(handles = it, onDismiss = {
                            onSharedFolderUpdated()
                            viewModel.clearLeaveFolderNodeIds()
                        })
                    }
                }
            }
        }

        activityChatContactBinding.root.addView(
            createNodeAttachmentView(
                activity = this,
                viewModel = nodeAttachmentViewModel,
            ) { message, id ->
                showSnackbarWithChat(message, id)
            }
        )
    }

    private fun modifyNickName() {
        if (contentContactProperties.nicknameText.text == getString(R.string.add_nickname)) {
            showConfirmationSetNickname(null)
        } else if (!viewModel.userEmail.isNullOrEmpty() && !contactNicknameBottomSheetDialogFragment.isBottomSheetDialogShown()) {
            contactNicknameBottomSheetDialogFragment =
                ContactNicknameBottomSheetDialogFragment().apply {
                    show(
                        supportFragmentManager,
                        contactNicknameBottomSheetDialogFragment?.tag
                    )
                }
        }
    }

    private fun updateViewBasedOnNetworkAvailability() {
        if (viewModel.isOnline()) {
            Timber.d("online -- network connection")
            with(contentContactProperties) {
                emailText.text?.let {
                    updateShareContactLayoutVisibility(shouldShow = true)
                    updateSharedFolderLayoutVisibility(shouldShow = true)
                    updateChatOptionsLayoutVisibility(shouldShow = true)
                } ?: run {
                    updateShareContactLayoutVisibility(shouldShow = false)
                    updateSharedFolderLayoutVisibility(shouldShow = false)
                    updateChatOptionsLayoutVisibility(shouldShow = false)
                }
            }

        } else {
            Timber.d("OFFLINE -- NO network connection")
            updateShareContactLayoutVisibility(shouldShow = false)
            updateSharedFolderLayoutVisibility(shouldShow = false)
            updateChatOptionsLayoutVisibility(shouldShow = true)
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
            contactPropertiesLayout.isVisible = shouldShow
            dividerChatHistoryLayout.isVisible = shouldShow
        }
    }

    private fun updateChatFilesSharedLayoutVisibility(shouldShow: Boolean) {
        with(contentContactProperties) {
            chatFilesSharedLayout.isVisible = shouldShow
            dividerChatFilesSharedLayout.isVisible = shouldShow
        }
    }

    private fun contactPropertiesClicked() {
        navigator.openManageChatHistoryActivity(
            context = this,
            email = viewModel.userEmail
        )
    }

    private fun sharedFilesClicked() {
        val nodeHistoryIntent = Intent(this, NodeAttachmentHistoryActivity::class.java)
        viewModel.chatId?.let {
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
        if (viewModel.userEmail.isNullOrEmpty()) {
            Timber.d("Selected contact NULL")
            return
        }
        val handle = viewModel.userHandle ?: return
        val intent = Intent(this, ChatExplorerActivity::class.java).apply {
            putExtra(Constants.USER_HANDLES, longArrayOf(handle))
        }
        shareContactResultLauncher.launch(intent)
    }

    private val shareContactResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val email = viewModel.userEmail
                if (!email.isNullOrEmpty()) {
                    val chatIds = data.getLongArrayExtra(SELECTED_CHATS) ?: longArrayOf()
                    val userHandles = data.getLongArrayExtra(SELECTED_USERS) ?: longArrayOf()
                    nodeAttachmentViewModel.attachContactToChat(
                        email = email,
                        chatIds = chatIds,
                        userHandles = userHandles
                    )
                }
            }
        }

    private fun verifyCredentialsClicked() {
        val intent = Intent(this, AuthenticityCredentialsActivity::class.java)
        intent.putExtra(Constants.EMAIL, viewModel.userEmail)
        startActivity(intent)
    }

    private fun visibilityStateIcon(userChatStatus: UserChatStatus) {
        if (stateToolbar == AppBarStateChangeListener.State.EXPANDED && userChatStatus.isValid()) {
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
            iconPackR.drawable.ic_message_arrow_up_medium_thin_outline,
            R.color.white
        )
        if (viewModel.isOnline()) {
            sendFileMenuItem?.isVisible = viewModel.isFromContacts
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
                        visibilityStateIcon(viewModel.userChatStatus)
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
                        visibilityStateIcon(viewModel.userChatStatus)
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
        drawableShare = ContextCompat.getDrawable(
            this,
            iconPackR.drawable.ic_folder_users_medium_thin_outline
        )?.mutate()
        drawableSend = ContextCompat.getDrawable(
            this,
            iconPackR.drawable.ic_message_arrow_up_medium_thin_outline
        )?.mutate()
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
                        getString(R.string.error_server_connection_problem),
                        -1
                    )
                    return true
                }
                sendFileToChat()
            }

            R.id.action_return_call -> {
                CallUtil.returnActiveCall(this)
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
        viewModel.userEmail?.let { selectFileResultLauncher.launch(it) }
            ?: run { Timber.w("Selected contact NULL") }
    }

    /**
     * Collecting Flows from ViewModel
     */
    private fun collectFlows() {
        collectFlow(viewModel.uiState) { contactInfoUiState: ContactInfoUiState ->
            if (contactInfoUiState.isUserRemoved) {
                finish()
            }
            if (contactInfoUiState.error != null) {
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.call_error),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
            }

            enableCallLayouts(contactInfoUiState.enableCallLayout)

            if (contactInfoUiState.callStatusChanged) {
                checkScreenRotationToShowCall()
            }
            handleOneOffEvents(contactInfoUiState)
            contactInfoUiState.snackBarMessage?.let {
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(it),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
                viewModel.onConsumeSnackBarMessageEvent()
            }
            contactInfoUiState.snackBarMessageString?.let {
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    it,
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
                viewModel.onConsumeSnackBarMessageEvent()
            }
            if (contactInfoUiState.isCopyInProgress) {
                if (statusDialog == null) {
                    statusDialog = createProgressDialog(this, getString(R.string.context_copying))
                }
            } else {
                statusDialog?.dismiss()
            }
            if (contactInfoUiState.nameCollisions.isNotEmpty()) {
                handleNodesNameCollisionResult(contactInfoUiState.nameCollisions)
                viewModel.markHandleNodeNameCollisionResult()
            }
            if (contactInfoUiState.moveRequestResult != null) {
                handleMovementResult(contactInfoUiState.moveRequestResult)
                viewModel.markHandleMoveRequestResult()
            }

            if (contactInfoUiState.shouldInitiateCall) {
                verifyPermissionAndJoinCall()
            }

            if (contactInfoUiState.navigateToMeeting) {
                navigateToMeetingActivity(contactInfoUiState)
            }

            updateVerifyCredentialsLayout(contactInfoUiState)
            updateUserStatusChanges(contactInfoUiState)
            updateBasicInfo(contactInfoUiState)
            setFoldersButtonText(contactInfoUiState.inShares)
            updateUI()
            if (contactInfoUiState.showForceUpdateDialog) {
                showForceUpdateAppDialog()
            }
        }

        collectFlow(viewModel.uiState.map { it.retentionTime }.distinctUntilChanged()) {
            it?.let {
                ChatUtil.updateRetentionTimeLayout(
                    contentContactProperties.retentionTimeText,
                    it,
                    this@ContactInfoActivity
                )
            }
        }

        collectFlow(waitingRoomManagementViewModel.state) { state ->
            state.snackbarString?.let {
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    it,
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
                waitingRoomManagementViewModel.onConsumeSnackBarMessageEvent()
            }

            if (state.shouldWaitingRoomBeShown) {
                waitingRoomManagementViewModel.onConsumeShouldWaitingRoomBeShownEvent()
                launchCallScreen()
            }
        }
    }

    /**
     * Show Force App Update Dialog
     */
    private fun showForceUpdateAppDialog() {
        if (forceAppUpdateDialog?.isShowing == true) return
        forceAppUpdateDialog = AlertDialogUtil.createForceAppUpdateDialog(this) {
            viewModel.onForceUpdateDialogDismissed()
        }
        forceAppUpdateDialog?.show()
    }

    private fun handleMovementResult(moveRequestResult: Result<MoveRequestResult>) {
        AlertDialogUtil.dismissAlertDialogIfExists(statusDialog)
        if (moveRequestResult.isSuccess) {
            val data = moveRequestResult.getOrThrow()
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                nodeMoveRequestMessageMapper(data),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
        } else {
            manageCopyMoveException(moveRequestResult.exceptionOrNull())
        }
    }

    private fun handleNodesNameCollisionResult(conflictNodes: List<NameCollision>) {
        if (conflictNodes.isNotEmpty()) {
            statusDialog?.dismiss()
            nameCollisionActivityLauncher.launch(ArrayList(conflictNodes))
        }
    }

    /**
     * Open meeting
     */
    private fun launchCallScreen() {
        val chatId = waitingRoomManagementViewModel.state.value.chatId
        MegaApplication.getInstance().openCallService(chatId)
        passcodeCheck.enablePassCode()

        val intent = Intent(this, MeetingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = MeetingActivity.MEETING_ACTION_IN
            putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
            putExtra(MeetingActivity.MEETING_BOTTOM_PANEL_EXPANDED, true)
        }
        startActivity(intent)
    }

    private fun navigateToMeetingActivity(contactInfoUiState: ContactInfoUiState) {
        val intentMeeting = Intent(this, MeetingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = MeetingActivity.MEETING_ACTION_IN
            putExtra(MeetingActivity.MEETING_CHAT_ID, contactInfoUiState.currentCallChatId)
            putExtra(
                MeetingActivity.MEETING_AUDIO_ENABLE,
                contactInfoUiState.currentCallAudioStatus
            )
            putExtra(
                MeetingActivity.MEETING_VIDEO_ENABLE,
                contactInfoUiState.currentCallVideoStatus
            )
        }
        viewModel.onConsumeNavigateToMeeting()
        startActivity(intentMeeting)
    }

    private fun verifyPermissionAndJoinCall() {
        val audio = hasPermissions(this, Manifest.permission.RECORD_AUDIO)

        if (!audio) {
            showSnackbar(
                Constants.PERMISSIONS_TYPE,
                getString(R.string.allow_acces_calls_subtitle_microphone),
                -1
            )
            return
        }

        var video = startVideo
        if (video) {
            video = hasPermissions(this, Manifest.permission.CAMERA)
        }
        viewModel.joinCall(video)
    }

    private fun handleOneOffEvents(contactInfoUiState: ContactInfoUiState) {
        when {
            contactInfoUiState.shouldNavigateToChat -> {
                viewModel.chatId?.let { navigateToChatActivity(it) }
                viewModel.onConsumeNavigateToChatEvent()
            }

            contactInfoUiState.isChatNotificationChange -> {
                chatNotificationsChange()
                viewModel.onConsumeChatNotificationChangeEvent()
            }

            contactInfoUiState.isStorageOverQuota -> {
                showOverDiskQuotaPaywallWarning()
                viewModel.onConsumeStorageOverQuotaEvent()
            }

            contactInfoUiState.isPushNotificationSettingsUpdated -> {
                viewModel.chatId?.let {
                    ChatUtil.checkSpecificChatNotifications(
                        it,
                        contentContactProperties.notificationSwitch,
                        contentContactProperties.notificationsMutedText,
                        this@ContactInfoActivity
                    )
                }
                viewModel.onConsumePushNotificationSettingsUpdateEvent()
            }

            contactInfoUiState.isNodeUpdated -> {
                sharedFoldersFragment?.let {
                    if (it.isVisible) {
                        viewModel.parentHandle?.let { handle -> it.setNodes(handle) }
                    }
                }
                viewModel.onConsumeNodeUpdateEvent()
            }
        }
    }

    private fun updateBasicInfo(contactInfoUiState: ContactInfoUiState) = with(contactInfoUiState) {
        contentContactProperties.emailText.text = contactInfoUiState.contactItem?.email
        collapsingAppBar.firstLineToolbar.text = primaryDisplayName
        contentContactProperties.nameText.apply {
            text = secondaryDisplayName
            isVisible = !secondaryDisplayName.isNullOrEmpty()
        }
        contentContactProperties.nicknameText.text =
            getString(contactInfoUiState.modifyNickNameTextId)
        updateAvatar(contactInfoUiState.avatar)
    }

    private fun updateUserStatusChanges(contactInfoUiState: ContactInfoUiState) {
        contactStateIcon =
            contactInfoUiState.userChatStatus.iconRes(isLightTheme = !Util.isDarkMode(this))
        collapsingAppBar.secondLineToolbar.apply {
            if (contactInfoUiState.userChatStatus.isValid()) {
                isVisible = true
                text = getString(contactInfoUiState.userChatStatus.text)
            } else isVisible = false
        }
        visibilityStateIcon(contactInfoUiState.userChatStatus)
        if (contactInfoUiState.userChatStatus.isAwayOrOffline()) {
            val formattedDate = TimeUtils.lastGreenDate(this, contactInfoUiState.lastGreen)
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
                verifyPermissionAndJoinCall()
            }

            Constants.REQUEST_CAMERA -> verifyPermissionAndJoinCall()
        }
    }

    private fun pickFolderToShare() {
        Timber.d("pickFolderToShare")
        viewModel.userEmail?.let {
            selectFolderResultLauncher.launch(it)
        } ?: run {
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                getString(R.string.error_sharing_folder),
                -1
            )
            Timber.w("Error sharing folder")
        }
    }

    private fun setOfflineAvatar() {
        val userEmail = viewModel.userEmail ?: return
        Timber.d("setOfflineAvatar")
        val avatar = buildAvatarFile("$userEmail.jpg")
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
            imageLayout.setBackgroundColor(AvatarUtil.getColorAvatar(viewModel.userHandle ?: -1L))
            toolbarImage.setImageBitmap(defaultAvatar)
        }
    }

    private fun startingACall(withVideo: Boolean) {
        startVideo = withVideo
        if (CallUtil.canCallBeStartedFromContactOption(this)) {
            verifyPermissionAndJoinCall()
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
            setImeActionLabel(getString(R.string.add_nickname), EditorInfo.IME_ACTION_DONE)
            hint = alias ?: getString(R.string.nickname_title)
            alias?.let {
                setText(alias)
                setSelection(this.length())
            }
        }

        val colorDisableButton = ContextCompat.getColor(this, R.color.color_text_on_color_disabled)
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
                getString(
                    viewModel.nickName
                        ?.let { R.string.edit_nickname } ?: run { R.string.add_nickname })
            )
            .setPositiveButton(getString(R.string.button_set)) { _, _ ->
                val name = emojiEditText.text.toString()
                if (name.isEmpty()) {
                    Timber.w("Input is empty")
                    emojiEditText.error = getString(R.string.invalid_string)
                    emojiEditText.requestFocus()
                } else {
                    viewModel.updateNickName(name)
                    setNicknameDialog?.dismiss()
                }
            }
        builder.setNegativeButton(getString(sharedR.string.general_dialog_cancel_button)) { _, _ -> setNicknameDialog?.dismiss() }
        builder.setView(layout)
        setNicknameDialog = builder.create().apply {
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).apply {
                isEnabled = false
                setTextColor(colorDisableButton)
            }
        }
    }

    private fun updateAvatar(avatar: Bitmap?) {
        avatar?.let {
            if (viewModel.isOnline()) {
                setAvatar(avatar)
            } else if (viewModel.chatId != null) {
                setOfflineAvatar()
            }
        } ?: run {
            setDefaultAvatar()
        }
    }

    private fun setAvatar(imBitmap: Bitmap?) {
        imBitmap?.let {
            collapsingAppBar.toolbarImage.setImageBitmap(it)
            if (!it.isRecycled) {
                val colorBackground = AvatarUtil.getDominantColor(it)
                collapsingAppBar.imageLayout.setBackgroundColor(colorBackground)
            }
        }
    }

    private fun showConfirmationRemoveContact() {
        Timber.d("showConfirmationRemoveContact")
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getQuantityString(R.plurals.title_confirmation_remove_contact, 1))
            .setMessage(resources.getQuantityString(R.plurals.confirmation_remove_contact, 1))
            .setPositiveButton(R.string.general_remove) { _, _ -> viewModel.removeContact() }
            .setNegativeButton(sharedR.string.general_dialog_cancel_button) { _, _ -> }
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
        megaApi.removeRequestListener(this)
    }

    /**
     * onResume life cycle callback
     */
    override fun onResume() {
        super.onResume()
        checkScreenRotationToShowCall()
        viewModel.getUserStatusAndRequestForLastGreen()
    }

    /**
     * onStart lifecycle callback
     */
    override fun onStart() {
        super.onStart()
        viewModel.refreshUserInfo()
    }

    private fun sharedFolderClicked() {
        val sharedFolderLayout =
            findViewById<View>(R.id.shared_folder_list_container) as RelativeLayout
        if (isShareFolderExpanded) {
            sharedFolderLayout.visibility = View.GONE
        } else {
            sharedFolderLayout.visibility = View.VISIBLE
            contentContactProperties.shareFoldersButton.setText(R.string.general_close)
            if (sharedFoldersFragment == null) {
                supportFragmentManager.commitNow {
                    sharedFoldersFragment = ContactSharedFolderFragment().apply {
                        setUserEmail(viewModel.userEmail)
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
            val chatId = viewModel.chatId
            if (chatId == null || chatId == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                updateChatFilesSharedLayoutVisibility(shouldShow = false)
                updateChatHistoryLayoutVisibility(shouldShow = false)
                retentionTimeText.isVisible = false
            } else {
                ChatUtil.updateRetentionTimeLayout(
                    retentionTimeText,
                    ChatUtil.getUpdatedRetentionTimeFromAChat(chatId),
                    this@ContactInfoActivity
                )
                if (viewModel.isOnline()) {
                    updateChatHistoryLayoutVisibility(shouldShow = true)
                } else {
                    updateChatHistoryLayoutVisibility(shouldShow = false)
                }
                updateChatFilesSharedLayoutVisibility(shouldShow = true)
                ChatUtil.checkSpecificChatNotifications(
                    chatId,
                    notificationSwitch,
                    notificationsMutedText,
                    this@ContactInfoActivity
                )
            }
            makeNotificationLayoutVisible()
        }
    }

    /**
     * Make the necessary actions when clicking on the Chat Notifications layout.
     */
    private fun chatNotificationsChange() {
        val chatId = viewModel.chatId ?: return
        if (contentContactProperties.notificationSwitch.isChecked) {
            ChatUtil.createMuteNotificationsAlertDialogOfAChat(this, chatId)
        } else {
            getPushNotificationSettingManagement().controlMuteNotificationsOfAChat(
                this,
                Constants.NOTIFICATIONS_ENABLED,
                chatId
            )
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
     * Method responsible for downloading file
     */
    fun downloadFile(nodes: List<MegaNode>) {
        startDownloadViewModel.onDownloadClicked(
            nodeIds = nodes.map { NodeId(it.handle) },
            isHighPriority = true,
            withStartMessage = true,
        )
        hideSelectMode()
    }

    /**
     * Method responsible for copying files
     */
    fun showCopy(handleList: ArrayList<Long>) {
        selectFolderToCopyLauncher.launch(handleList.toLongArray())
    }

    private fun setFoldersButtonText(nodes: List<UnTypedNode>) {
        contentContactProperties.apply {
            shareFoldersButton.text = resources.getQuantityString(
                R.plurals.num_folders_with_parameter,
                nodes.size,
                nodes.size
            )
            val isClickable = nodes.isNotEmpty()
            shareFoldersButton.isClickable = isClickable
            sharedFoldersLayout.isClickable = isClickable
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
     * @param state [ContactInfoUiState].
     */
    private fun updateVerifyCredentialsLayout(state: ContactInfoUiState) {
        contentContactProperties.apply {
            if (!state.contactItem?.email.isNullOrEmpty()) {
                verifyCredentialsLayout.isVisible = true
                if (state.areCredentialsVerified) {
                    verifyCredentialsInfo.setText(R.string.contact_verify_credentials_verified_text)
                    verifyCredentialsInfoIcon.isVisible = true
                } else {
                    verifyCredentialsInfo.setText(R.string.contact_verify_credentials_not_verified_text)
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
        viewModel.onConsumeChatCallStatusChangeEvent()
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

    private fun hideSelectMode() {
        sharedFoldersFragment?.takeIf { it.isVisible }?.apply {
            clearSelections()
            hideMultipleSelect()
        }
    }
}
