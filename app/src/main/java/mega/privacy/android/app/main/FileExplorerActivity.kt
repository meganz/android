package mega.privacy.android.app.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityFileExplorerBinding
import mega.privacy.android.app.extensions.consumeInsetsWithToolbar
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.CreateChatListener
import mega.privacy.android.app.listeners.CreateFolderListener
import mega.privacy.android.app.listeners.GetAttrUserListener
import mega.privacy.android.app.main.FileExplorerActivity.Companion.CAMERA
import mega.privacy.android.app.main.FileExplorerActivity.Companion.COPY
import mega.privacy.android.app.main.FileExplorerActivity.Companion.IMPORT
import mega.privacy.android.app.main.FileExplorerActivity.Companion.MOVE
import mega.privacy.android.app.main.FileExplorerActivity.Companion.SAVE
import mega.privacy.android.app.main.FileExplorerActivity.Companion.SELECT
import mega.privacy.android.app.main.FileExplorerActivity.Companion.SELECT_CAMERA_FOLDER
import mega.privacy.android.app.main.FileExplorerActivity.Companion.SHARE_LINK
import mega.privacy.android.app.main.FileExplorerActivity.Companion.UPLOAD
import mega.privacy.android.app.main.adapters.FileExplorerPagerAdapter
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.main.legacycontact.AddContactActivity.Companion.ALLOW_ADD_PARTICIPANTS
import mega.privacy.android.app.main.legacycontact.AddContactActivity.Companion.EXTRA_CHAT_LINK
import mega.privacy.android.app.main.legacycontact.AddContactActivity.Companion.EXTRA_CHAT_TITLE
import mega.privacy.android.app.main.legacycontact.AddContactActivity.Companion.EXTRA_CONTACTS
import mega.privacy.android.app.main.legacycontact.AddContactActivity.Companion.EXTRA_CONTACT_TYPE
import mega.privacy.android.app.main.legacycontact.AddContactActivity.Companion.EXTRA_EKR
import mega.privacy.android.app.main.legacycontact.AddContactActivity.Companion.EXTRA_ONLY_CREATE_GROUP
import mega.privacy.android.app.main.listeners.CreateGroupChatWithPublicLink
import mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerFragment
import mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerListItem
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment.Companion.newInstance
import mega.privacy.android.app.presentation.documentscanner.dialogs.DiscardScanUploadingWarningDialog
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.view.createStartTransferView
import mega.privacy.android.app.presentation.upload.UploadDestinationActivity
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ColorUtils.tintIcon
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CONTACT_TYPE_MEGA
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IMPORT_TO
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil.IS_NEW_FOLDER_DIALOG_SHOWN
import mega.privacy.android.app.utils.MegaNodeDialogUtil.NEW_FOLDER_DIALOG_TEXT
import mega.privacy.android.app.utils.MegaNodeDialogUtil.checkNewFolderDialogState
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showNewFolderDialog
import mega.privacy.android.app.utils.MegaNodeUtil.cloudRootHandle
import mega.privacy.android.app.utils.MegaNodeUtil.existsMyChatFilesFolder
import mega.privacy.android.app.utils.MegaNodeUtil.myChatFilesFolder
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatPresenceLastGreenUpdatesUseCase
import mega.privacy.android.domain.usecase.file.CheckFileNameCollisionsUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.DocumentScannerUploadingImageToChatEvent
import mega.privacy.mobile.analytics.event.DocumentScannerUploadingImageToCloudDriveEvent
import mega.privacy.mobile.analytics.event.DocumentScannerUploadingPDFToChatEvent
import mega.privacy.mobile.analytics.event.DocumentScannerUploadingPDFToCloudDriveEvent
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiJava
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
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import java.io.File
import javax.inject.Inject


/**
 * Activity used for several purposes like import content to the cloud, copies or movements.
 *
 * @property monitorChatPresenceLastGreenUpdatesUseCase     [MonitorChatPresenceLastGreenUpdatesUseCase]
 * @property copyNodeUseCase           [CopyNodeUseCase]
 * @property checkFileNameCollisionsUseCase [CheckFileNameCollisionsUseCase]
 * @property loginMutex                Mutex.
 * @property isList                    True if the view is in list mode, false if it is in grid mode.
 * @property mode                      Mode for opening the file explorer: [UPLOAD], [MOVE], [COPY], [CAMERA], [IMPORT], [SELECT], [SELECT_CAMERA_FOLDER], [SHARE_LINK] or [SAVE]
 * @property isMultiselect             True if it should allow multiple selection, false otherwise.
 * @property isSelectFile              True if is selecting a file, false otherwise.
 * @property querySearch               Type query for a search.
 * @property parentHandleIncoming      Parent handle of the incoming fragment.
 * @property parentHandleCloud         Parent handle of the cloud fragment.
 * @property deepBrowserTree           Deep browser tree of the incoming fragment.
 * @property shouldRestartSearch       True if should restart the search, false otherwise.
 */
@AndroidEntryPoint
class FileExplorerActivity : PasscodeActivity(), MegaRequestListenerInterface,
    MegaGlobalListenerInterface, MegaChatRequestListenerInterface, View.OnClickListener,
    ActionNodeCallback, SnackbarShower {

    /**
     * The Application Theme Mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var monitorChatPresenceLastGreenUpdatesUseCase: MonitorChatPresenceLastGreenUpdatesUseCase

    @Inject
    lateinit var checkFileNameCollisionsUseCase: CheckFileNameCollisionsUseCase

    @Inject
    lateinit var copyNodeUseCase: CopyNodeUseCase

    @Inject
    @LoginMutex
    lateinit var loginMutex: Mutex

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val viewModel by viewModels<FileExplorerViewModel>()

    private lateinit var binding: ActivityFileExplorerBinding
    private val isFromUploadDestinationActivity by lazy {
        intent.hasExtra(UploadDestinationActivity.EXTRA_NAVIGATION)
    }

    var isList = true
        private set
    var mode = 0
    var isMultiselect = false
        private set
    var isSelectFile = false
    var querySearch: String? = ""
        private set
    var parentHandleIncoming: Long = 0
    var parentHandleCloud: Long = 0
    var deepBrowserTree = 0
    var shouldRestartSearch = false

    private var prefs: MegaPreferences? = null
    private var parentMoveCopy: MegaNode? = null
    private var createFolderMenuItem: MenuItem? = null
    private var newChatMenuItem: MenuItem? = null
    private var searchMenuItem: MenuItem? = null
    private var fragmentHandle: Long = -1
    private var credentials: UserCredentials? = null
    private var moveFromHandles: LongArray? = null
    private var copyFromHandles: LongArray? = null
    private var importChatHandles: LongArray? = null
    private var selectedContacts: ArrayList<String>? = null
    private var folderSelected = false
    private var handler: Handler? = null
    private var tabShown = CLOUD_TAB
    private val chatListItems: ArrayList<MegaChatRoom> = ArrayList()
    private var cDriveExplorer: CloudDriveExplorerFragment? = null
    private var iSharesExplorer: IncomingSharesExplorerFragment? = null
    private var chatExplorer: ChatExplorerFragment? = null
    private var importFileFragment: ImportFilesFragment? = null
    private var statusDialog: AlertDialog? = null
    private var newFolderDialog: AlertDialog? = null
    private var mTabsAdapterExplorer: FileExplorerPagerAdapter? = null
    private var nodes: ArrayList<MegaNode>? = null
    private var importFileF = false
    private var importFragmentSelected = -1
    private var action: String? = null
    private var myChatFilesNode: MegaNode? = null
    private val attachNodes: ArrayList<MegaNode> = ArrayList()
    private val uploadDocuments: ArrayList<DocumentEntity> = ArrayList()
    private var filesChecked = 0
    private var searchView: SearchView? = null
    private var needLogin = false
    private var isSearchExpanded = false
    private var collapsedByClick = false
    private var pendingToOpenSearchView = false
    private var pendingToAttach = 0
    private var totalAttached = 0
    private var totalErrors = 0
    private var queryAfterSearch: String? = null
    private var currentAction: String? = null
    private var bottomSheetDialogFragment: BottomSheetDialogFragment? = null
    private var parentHandle: Long = 0

    private val nameCollisionActivityLauncher = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        viewModel.setIsAskingForCollisionsResolution(isAskingForCollisionsResolution = false)
        backToCloud(
            if (result != null) parentHandle else INVALID_HANDLE,
            result
        )
    }

    private val elevation by lazy { resources.getDimension(R.dimen.toolbar_elevation) }

    private lateinit var createChatLauncher: ActivityResultLauncher<Intent>

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            retryConnectionsAndSignalPresence()
            cDriveExplorer = cloudExplorerFragment
            iSharesExplorer = incomingExplorerFragment

            if (importFileF && importFragmentSelected != CLOUD_FRAGMENT) {
                when (importFragmentSelected) {
                    CHAT_FRAGMENT -> {
                        if (ACTION_UPLOAD_TO_CHAT == action) {
                            if (chatExplorer != null && chatExplorer?.isSelectMode == true) {
                                chatExplorer?.clearSelections()
                            } else {
                                viewModel.handleBackNavigation()
                            }
                        } else {
                            chatExplorer = chatExplorerFragment

                            if (chatExplorer != null) {
                                chatExplorer?.clearSelections()
                                showFabButton(false)
                                if (isFromUploadDestinationActivity) {
                                    finishAndRemoveTask()
                                } else {
                                    chooseFragment(IMPORT_FRAGMENT)
                                }
                            }
                        }
                    }

                    IMPORT_FRAGMENT -> {
                        viewModel.handleBackNavigation()
                    }
                }
            } else if (isCloudVisible) {
                if (cDriveExplorer?.onBackPressed() == 0) {
                    performImportFileBack()
                }
            } else if (isIncomingVisible) {
                if (iSharesExplorer?.onBackPressed() == 0) {
                    performImportFileBack()
                }
            } else {
                finish()
            }
        }
    }

    override fun onRequestStart(api: MegaChatApiJava, request: MegaChatRequest) {}

    override fun onRequestUpdate(api: MegaChatApiJava, request: MegaChatRequest) {}

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        Timber.d("onRequestFinish(CHAT)")
        when (request.type) {
            MegaChatRequest.TYPE_CREATE_CHATROOM -> {
                Timber.d("Create chat request finish.")
                onRequestFinishCreateChat(e.errorCode)
            }

            MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE -> {
                Timber.d("Attach file request finish.")
                if (e.errorCode == MegaChatError.ERROR_OK) {
                    totalAttached++
                } else {
                    totalErrors++
                }

                if (totalAttached + totalErrors == pendingToAttach) {
                    finishFileExplorer()
                    if (totalErrors == 0 || totalAttached > 0) {
                        val intent = Intent(this, ManagerActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        intent.action = Constants.ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE
                        if (chatListItems.size == 1) {
                            intent.putExtra(Constants.CHAT_ID, chatListItems[0].chatId)
                        }
                        startActivity(intent)
                    } else {
                        showSnackbar(getString(R.string.files_send_to_chat_error))
                    }
                }
            }
        }
    }

    override fun onRequestTemporaryError(
        api: MegaChatApiJava,
        request: MegaChatRequest,
        e: MegaChatError,
    ) {
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.fragmentContainerFileExplorer, content, chatId)
    }

    private fun onProcessAsyncInfo(documents: List<DocumentEntity>?) {
        if (documents.isNullOrEmpty()) {
            Timber.w("Selected items list is null or empty.")
            return
        }

        if (needLogin) {
            val loginIntent = intent.setClass(this@FileExplorerActivity, LoginActivity::class.java)
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    putExtra(EXTRA_FROM_SHARE, true)
                    // close previous login page
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    action = Constants.ACTION_FILE_EXPLORER_UPLOAD
                }

            needLogin = false
            startActivity(loginIntent)
            finish()
            return
        }

        if (action != null && intent != null) {
            intent.action = action
        }

        if (chatListItems.isNotEmpty()) {
            onIntentProcessed(documents)
        } else if (importFileF) {
            when {
                importFragmentSelected != -1 -> chooseFragment(importFragmentSelected)
                ACTION_UPLOAD_TO_CHAT == action -> chooseFragment(CHAT_FRAGMENT)
                else -> chooseFragment(IMPORT_FRAGMENT)
            }

            dismissAlertDialogIfExists(statusDialog)
        } else {
            onIntentProcessed()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        when (keyCode) {
            KeyEvent.KEYCODE_MENU -> true
            else -> super.onKeyDown(keyCode, event)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        Timber.d("onCreate first")
        super.onCreate(savedInstanceState)
        credentials = runBlocking {
            runCatching {
                getAccountCredentialsUseCase()
            }.getOrNull()
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        megaApi.addGlobalListener(this)

        createChatLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode != RESULT_OK) {
                    Timber.d("Result is not OK")
                    return@registerForActivityResult
                }
                val vaal = intent.getStringArrayListExtra(EXTRA_CONTACTS)
                result.data?.let { intent ->
                    intent.getStringArrayListExtra(EXTRA_CONTACTS)
                        ?.let { contactsData ->
                            if (contactsData.size == 1) {
                                val user = megaApi.getContact(contactsData[0])

                                if (user != null) {
                                    Timber.d("Chat with contact: %s", contactsData.size)
                                    startOneToOneChat(user)
                                }
                            } else {
                                Timber.d("Create GROUP chat")
                                val peers = MegaChatPeerList.createInstance()

                                for (i in contactsData.indices) {
                                    val user = megaApi.getContact(contactsData[i])
                                    if (user != null) {
                                        peers.addPeer(user.handle, MegaChatPeerList.PRIV_STANDARD)
                                    }
                                }

                                Timber.d("create group chat with participants: %s", peers.size())
                                val chatTitle = intent.getStringExtra(EXTRA_CHAT_TITLE)
                                val isEKR = intent.getBooleanExtra(EXTRA_EKR, false)
                                val allowAddParticipants =
                                    intent.getBooleanExtra(ALLOW_ADD_PARTICIPANTS, false)

                                if (isEKR) {
                                    megaChatApi.createGroupChat(
                                        peers,
                                        chatTitle,
                                        false,
                                        false,
                                        allowAddParticipants,
                                        this
                                    )
                                } else {
                                    val chatLink = intent.getBooleanExtra(EXTRA_CHAT_LINK, false)

                                    if (chatLink) {
                                        if (chatTitle != null && chatTitle.isNotEmpty()) {
                                            megaChatApi.createPublicChat(
                                                peers,
                                                chatTitle,
                                                false,
                                                false,
                                                allowAddParticipants,
                                                CreateGroupChatWithPublicLink(this, chatTitle)
                                            )
                                        } else {
                                            Util.showAlert(
                                                this,
                                                getString(R.string.message_error_set_title_get_link),
                                                null
                                            )
                                        }
                                    } else {
                                        megaChatApi.createPublicChat(
                                            peers,
                                            chatTitle,
                                            false,
                                            false,
                                            allowAddParticipants,
                                            this
                                        )
                                    }
                                }
                            }
                        }
                }
            }

        setupObservers()

        if (savedInstanceState != null) {
            with(savedInstanceState) {
                Timber.d("Bundle is NOT NULL")
                parentHandleCloud = getLong("parentHandleCloud", -1)
                Timber.d("savedInstanceState -> parentHandleCloud: %s", parentHandleCloud)
                parentHandleIncoming = getLong("parentHandleIncoming", -1)
                Timber.d("savedInstanceState -> parentHandleIncoming: %s", parentHandleIncoming)
                deepBrowserTree = getInt("deepBrowserTree", 0)
                Timber.d("savedInstanceState -> deepBrowserTree: %s", deepBrowserTree)
                importFileF = getBoolean("importFileF", false)
                importFragmentSelected = getInt("importFragmentSelected", -1)
                action = getString("action", null)
                chatExplorer =
                    this@FileExplorerActivity.supportFragmentManager.getFragment(
                        savedInstanceState,
                        "chatExplorerFragment"
                    ) as ChatExplorerFragment?
                querySearch = getString("querySearch", "")
                isSearchExpanded = getBoolean("isSearchExpanded", isSearchExpanded)
                pendingToAttach = getInt("pendingToAttach", 0)
                totalAttached = getInt("totalAttached", 0)
                totalErrors = getInt("totalErrors", 0)
                shouldRestartSearch = getBoolean(SHOULD_RESTART_SEARCH, false)
                queryAfterSearch = getString(QUERY_AFTER_SEARCH, null)
                currentAction = getString(CURRENT_ACTION, null)

                if (isSearchExpanded) {
                    pendingToOpenSearchView = true
                }
                if (getBoolean(IS_NEW_FOLDER_DIALOG_SHOWN, false)) {
                    newFolderDialog =
                        showNewFolderDialog(
                            this@FileExplorerActivity, this@FileExplorerActivity,
                            currentParentNode, savedInstanceState.getString(NEW_FOLDER_DIALOG_TEXT)
                        )
                }
            }
        } else {
            Timber.d("Bundle is NULL")
            parentHandleCloud = -1
            parentHandleIncoming = -1
            deepBrowserTree = 0
            importFileF = false
            importFragmentSelected = -1
            action = null
            pendingToAttach = 0
            totalAttached = 0
            totalErrors = 0
        }

        prefs = dbH.preferences

        isList = if (prefs == null || prefs?.preferredViewList == null) {
            true
        } else {
            java.lang.Boolean.parseBoolean(prefs?.preferredViewList)
        }

        if (credentials == null) {
            Timber.w("User credentials NULL")
            if (viewModel.isImportingText(intent)) {
                startActivity(
                    Intent(this, LoginActivity::class.java)
                        .putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                        .putExtra(Intent.EXTRA_TEXT, intent.getStringExtra(Intent.EXTRA_TEXT))
                        .putExtra(Intent.EXTRA_SUBJECT, intent.getStringExtra(Intent.EXTRA_SUBJECT))
                        .putExtra(Intent.EXTRA_EMAIL, intent.getStringExtra(Intent.EXTRA_EMAIL))
                        .setAction(Constants.ACTION_FILE_EXPLORER_UPLOAD)
                        .setType(Constants.TYPE_TEXT_PLAIN)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )

                finish()
            } else {
                needLogin = true
                viewModel.ownFilePrepareTask(this, intent)
                createAndShowProgressDialog(
                    false,
                    resources.getQuantityString(R.plurals.upload_prepare, 1)
                )
            }
            return
        } else {
            Timber.d("User has credentials")
        }

        if (savedInstanceState != null) {
            folderSelected = savedInstanceState.getBoolean("folderSelected", false)
        }
        enableEdgeToEdge()
        binding = ActivityFileExplorerBinding.inflate(layoutInflater)
        consumeInsetsWithToolbar(customToolbar = binding.appBarLayoutExplorer)
        setContentView(binding.root)
        addStartUploadTransferView()

        setSupportActionBar(binding.toolbarExplorer)
        supportActionBar?.hide()

        binding.fabFileExplorer.setOnClickListener(this)
        showFabButton(false)
        binding.explorerTabsPager.offscreenPageLimit = 3

        if (megaApi.rootNode == null) {
            Timber.d("hide action bar")
            if (!loginMutex.isLocked) {
                (supportActionBar ?: return).hide()
                with(binding) {
                    slidingTabsFileExplorer.isVisible = false
                    explorerTabsPager.isVisible = false
                    fileLoginQuerySignupLinkText.isVisible = false
                    fileLoginConfirmAccountText.isVisible = false
                    fileLoggingInLayout.isVisible = true
                    fileLoginProgressBar.isVisible = true
                    fileLoginFetchingNodesBar.isVisible = false
                    fileLoginLoggingInText.isVisible = true
                    fileLoginGeneratingKeysText.isVisible = false
                    fileLoginPrepareNodesText.isVisible = false
                }

                val gSession = credentials?.session
                lifecycleScope.launch {
                    loginMutex.lock()
                    ChatUtil.initMegaChatApi(gSession, this@FileExplorerActivity)
                    megaApi.fastLogin(gSession, this@FileExplorerActivity)
                }
            } else {
                megaApi.addRequestListener(this)
                megaChatApi.addChatRequestListener(this)
                Timber.w("Another login is proccessing")
            }
        } else {
            afterLoginAndFetch()
        }

        handleImportFromUploadDestination()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )

        binding.discardScanUploadingWarningDialogComposeView.setContent {
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val isDark = themeMode.isDarkMode()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            OriginalTheme(isDark = isDark) {
                if (state.isUploadingScans && state.isScanUploadingAborted) {
                    DiscardScanUploadingWarningDialog(
                        hasMultipleScans = state.hasMultipleScans,
                        onWarningAcknowledged = {
                            viewModel.setIsScanUploadingAborted(false)
                            viewModel.setShouldFinishScreen(true)
                        },
                        onWarningDismissed = {
                            viewModel.setIsScanUploadingAborted(false)
                        },
                    )
                }
            }
        }
    }

    private fun handleImportFromUploadDestination() {
        if (isFromUploadDestinationActivity) {
            val fragment =
                intent.getIntExtra(UploadDestinationActivity.EXTRA_NAVIGATION, CLOUD_FRAGMENT)
            importFileF = true
            importFragmentSelected = fragment
            chooseFragment(fragment)
            viewModel.ownFilePrepareTask(this, intent)
            createAndShowProgressDialog(
                false,
                resources.getQuantityString(R.plurals.upload_prepare, 1)
            )
        }
    }

    private fun setupObservers() {
        this.lifecycleScope.launch {
            val documents = viewModel.uiState
                .mapNotNull { it.documents.takeIf { it.isNotEmpty() } }
                .flowWithLifecycle(this@FileExplorerActivity.lifecycle, Lifecycle.State.STARTED)
                .catch {
                    Timber.e(it)
                }.firstOrNull()
            onProcessAsyncInfo(documents)
        }
        viewModel.textInfo.observe(this) { dismissAlertDialogIfExists(statusDialog) }

        collectFlow(viewModel.uiState) { fileExplorerState ->
            if (fileExplorerState.shouldFinishScreen) {
                finishAndRemoveTask()
                viewModel.setShouldFinishScreen(false)
            }
        }

        collectFlow(viewModel.copyTargetPathFlow) {
            if (it != null) {
                setView(SHOW_TABS, true)
                viewModel.resetCopyTargetPathState()
            }
        }

        collectFlow(viewModel.moveTargetPathFlow) {
            if (it != null) {
                setView(SHOW_TABS, true)
                viewModel.resetMoveTargetPathState()
            }
        }
    }

    private fun afterLoginAndFetch() {
        handler = Handler(Looper.getMainLooper())
        Timber.d("SHOW action bar")

        val upButtonRes =
            if (intent.action == ACTION_SAVE_TO_CLOUD || intent.action == ACTION_UPLOAD_TO_CHAT) {
                // Use the "X" Button when accessing this Activity from Document Scanner
                R.drawable.ic_close_white
            } else {
                R.drawable.ic_arrow_back_white
            }

        supportActionBar?.apply {
            show()
            Timber.d("supportActionBar.setHomeAsUpIndicator")
            setHomeAsUpIndicator(tintIcon(this@FileExplorerActivity, upButtonRes))
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        if (intent != null && intent.action != null) {
            selectedContacts = intent.getStringArrayListExtra(Constants.SELECTED_CONTACTS)
            Timber.d("intent OK: %s", intent.action)
            currentAction = intent.action
            val title: String?

            when (intent.action) {
                ACTION_SELECT_FOLDER_TO_SHARE -> {
                    Timber.d("action = ACTION_SELECT_FOLDER_TO_SHARE")
                    //Just show Cloud Drive, no INCOMING tab , no need of tabhost
                    mode = SELECT
                    title = getString(R.string.title_share_folder_explorer)
                    setView(CLOUD_TAB, false)
                    tabShown = NO_TABS
                }

                ACTION_MULTISELECT_FILE -> {
                    Timber.d("action = ACTION_MULTISELECT_FILE")
                    //Just show Cloud Drive, no INCOMING tab , no need of tabhost
                    mode = SELECT
                    isSelectFile = true
                    isMultiselect = true
                    title = resources.getQuantityString(R.plurals.plural_select_file, 10)
                    setView(SHOW_TABS, true)
                }

                ACTION_PICK_MOVE_FOLDER -> {
                    Timber.d("ACTION_PICK_MOVE_FOLDER")
                    mode = MOVE
                    moveFromHandles = intent.getLongArrayExtra("MOVE_FROM")

                    viewModel.getMoveTargetPath()
                    moveFromHandles?.let { handles ->
                        if (handles.isNotEmpty()) {
                            parentMoveCopy =
                                megaApi.getParentNode(megaApi.getNodeByHandle(handles[0]))
                        }
                    }

                    title = getString(R.string.title_share_folder_explorer)
                }

                ACTION_PICK_COPY_FOLDER -> {
                    Timber.d("ACTION_PICK_COPY_FOLDER")
                    mode = COPY
                    copyFromHandles = intent.getLongArrayExtra("COPY_FROM")

                    viewModel.getCopyTargetPath()
                    copyFromHandles?.let { handles ->
                        parentMoveCopy =
                            megaApi.getParentNode(megaApi.getNodeByHandle(handles[0]))
                    }

                    title = getString(R.string.title_share_folder_explorer)
                }

                ACTION_CHOOSE_MEGA_FOLDER_SYNC -> {
                    Timber.d("action = ACTION_CHOOSE_MEGA_FOLDER_SYNC")
                    mode = SELECT_CAMERA_FOLDER
                    title = getString(R.string.title_share_folder_explorer)
                    setView(SHOW_TABS, true)
                }

                ACTION_PICK_IMPORT_FOLDER -> {
                    mode = IMPORT
                    importChatHandles = intent.getLongArrayExtra("HANDLES_IMPORT_CHAT")
                    title = getString(R.string.title_share_folder_explorer)
                    setView(SHOW_TABS, true)
                }

                ACTION_SAVE_TO_CLOUD -> {
                    Timber.d("action = SAVE to Cloud Drive")
                    mode = SAVE
                    isSelectFile = false
                    parentHandleCloud = intent.getLongExtra(EXTRA_PARENT_HANDLE, INVALID_HANDLE)
                    title = getString(R.string.title_upload_explorer)

                    with(binding) {
                        slidingTabsFileExplorer.isVisible = false
                        explorerTabsPager.isVisible = false
                        cloudDriveFrameLayout.isVisible = false
                    }
                    chooseFragment(CLOUD_FRAGMENT)
                }

                ACTION_IMPORT_ALBUM -> {
                    mode = ALBUM_IMPORT
                    title = getString(R.string.section_cloud_drive)
                    setView(CLOUD_TAB, false)
                    tabShown = NO_TABS
                }

                else -> {
                    configureView()
                    title = getString(R.string.title_upload_explorer)
                    importFileF = true
                    viewModel.ownFilePrepareTask(this, intent)
                    chooseFragment(
                        if (intent.action == ACTION_UPLOAD_TO_CHAT) {
                            CHAT_FRAGMENT
                        } else {
                            IMPORT_FRAGMENT
                        }
                    )
                }
            }

            title.let { supportActionBar?.title = it }
        } else {
            Timber.e("intent error")
        }
    }

    private fun configureView() {
        Timber.d("action = UPLOAD")
        mode = UPLOAD
        action = intent.action
        createAndShowProgressDialog(
            false,
            resources.getQuantityString(R.plurals.upload_prepare, 1)
        )

        with(binding) {
            cloudDriveFrameLayout.isVisible = true
            slidingTabsFileExplorer.isVisible = false
            explorerTabsPager.isVisible = false
        }

        tabShown = NO_TABS
    }

    /**
     * Updates the UI for showing tabs and removes the chat one if required.
     *
     * @param removeChatTab True if should remove the chat tab, false otherwise.
     */
    private fun updateAdapterExplorer(removeChatTab: Boolean) {
        with(binding) {
            slidingTabsFileExplorer.isVisible = true
            explorerTabsPager.isVisible = true

            val position =
                if (mTabsAdapterExplorer != null) explorerTabsPager.currentItem
                else if (viewModel.latestCopyTargetPath != null) viewModel.latestCopyTargetPathTab
                else if (viewModel.latestMoveTargetPath != null) viewModel.latestMoveTargetPathTab
                else 0

            mTabsAdapterExplorer = FileExplorerPagerAdapter(
                supportFragmentManager,
                this@FileExplorerActivity.lifecycle
            )
            explorerTabsPager.adapter = mTabsAdapterExplorer
            explorerTabsPager.currentItem = position

            TabLayoutMediator(
                slidingTabsFileExplorer,
                explorerTabsPager
            ) { tab: TabLayout.Tab, tabPosition: Int ->
                tab.text = when (tabPosition) {
                    1 -> getString(R.string.tab_incoming_shares)
                    2 -> getString(R.string.section_chat)
                    0 -> getString(R.string.section_cloud_drive)
                    else -> getString(R.string.section_cloud_drive)
                }
            }.attach()

            if ((mTabsAdapterExplorer?.itemCount ?: return@with) > 2 && removeChatTab) {
                mTabsAdapterExplorer?.tabRemoved = true
                slidingTabsFileExplorer.removeTabAt(2)
                mTabsAdapterExplorer?.notifyDataSetChanged()
            }
        }
    }

    /**
     * Calls the respective ViewModel function to handle the Back Navigation logic
     */
    fun handleBackNavigation() = viewModel.handleBackNavigation()

    /**
     * Updates the UI for showing tabs or only a fragment.
     * If it has to show tabs, removes the chat one if required.
     *
     * @param tab           SHOW_TABS if should show tabs, else the fragment to show.
     * @param removeChatTab True if should remove the chat tab, false otherwise.
     */
    private fun setView(tab: Int, removeChatTab: Boolean) {
        Timber.d("setView %s", tab)
        when (tab) {
            CLOUD_TAB -> {
                if (cDriveExplorer == null) {
                    cDriveExplorer = CloudDriveExplorerFragment()
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.cloudDriveFrameLayout, cDriveExplorer ?: return, "cDriveExplorer")
                    .commitNowAllowingStateLoss()

                with(binding) {
                    cloudDriveFrameLayout.isVisible = true
                    slidingTabsFileExplorer.isVisible = false
                    explorerTabsPager.isVisible = false
                }
            }

            SHOW_TABS -> {
                tabShown = SHOW_TABS
                if (mTabsAdapterExplorer == null || importFileF) {
                    updateAdapterExplorer(removeChatTab)
                }
                binding.explorerTabsPager.registerOnPageChangeCallback(object :
                    ViewPager2.OnPageChangeCallback() {

                    override fun onPageSelected(position: Int) {
                        Timber.d("Position:%s", position)
                        invalidateOptionsMenu()
                        changeTitle()
                        checkFragmentScroll(position)
                        if (!isMultiselect) {
                            return
                        }

                        if (isSearchExpanded && !pendingToOpenSearchView) {
                            clearQuerySearch()
                            collapseSearchView()
                        }

                        if (position == 0) {
                            iSharesExplorer?.hideMultipleSelect()
                        } else if (position == 1) {
                            cDriveExplorer?.hideMultipleSelect()
                        }
                    }

                })
            }
        }
    }

    private fun checkFragmentScroll(position: Int) =
        mTabsAdapterExplorer?.getFragment(position)?.let {
            (it as CheckScrollInterface).checkScroll()
        }


    /**
     * Shows the chosen fragment.
     *
     * @param fragment The chosen fragment.
     */
    fun chooseFragment(fragment: Int) {
        importFragmentSelected = fragment
        val ft = supportFragmentManager.beginTransaction()

        when (fragment) {
            CLOUD_FRAGMENT -> {
                binding.cloudDriveFrameLayout.isVisible = false
                setView(SHOW_TABS, true)
            }

            CHAT_FRAGMENT -> {
                if (chatExplorer == null) {
                    chatExplorer = ChatExplorerFragment()
                }
                ft.replace(R.id.cloudDriveFrameLayout, chatExplorer ?: return, "chatExplorer")
            }

            IMPORT_FRAGMENT -> {
                tabShown = NO_TABS

                with(binding) {
                    slidingTabsFileExplorer.isVisible = false
                    explorerTabsPager.isVisible = false
                    cloudDriveFrameLayout.isVisible = true
                }

                if (importFileFragment == null) {
                    importFileFragment = ImportFilesFragment()
                }

                ft.replace(
                    R.id.cloudDriveFrameLayout,
                    importFileFragment ?: return,
                    "importFileFragment"
                )
            }
        }

        ft.commitNowAllowingStateLoss()
        invalidateOptionsMenu()
        changeTitle()
    }

    /**
     * Updates the fab button visibility.
     *
     * @param show True if should show it, false if should hide it.
     */
    fun showFabButton(show: Boolean) {
        binding.fabFileExplorer.isVisible = show
    }

    /**
     * Updates the elevation.
     *
     * @param elevate       True if should show elevation, false otherwise.
     * @param fragmentIndex Index of the current fragment.
     */
    fun changeActionBarElevation(elevate: Boolean, fragmentIndex: Int) {
        if (!isCurrentFragment(fragmentIndex)) return
        binding.appBarLayoutExplorer.elevation = if (elevate) elevation else 0f
    }

    private fun isCurrentFragment(index: Int): Boolean =
        if (tabShown == NO_TABS) true
        else when (index) {
            CLOUD_FRAGMENT -> tabShown == CLOUD_TAB
            CHAT_FRAGMENT -> tabShown == CHAT_TAB
            INCOMING_FRAGMENT -> tabShown == INCOMING_TAB
            else -> false
        }

    private val isSearchMultiselect: Boolean
        get() {
            if (isMultiselect) {
                cDriveExplorer = cloudExplorerFragment
                iSharesExplorer = incomingExplorerFragment
                return isCloudVisible || isIncomingVisible
            }
            return false
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu")

        // Inflate the menu items for use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.file_explorer_action, menu)
        searchMenuItem = menu.findItem(R.id.cab_menu_search)
        createFolderMenuItem = menu.findItem(R.id.cab_menu_create_folder)
        newChatMenuItem = menu.findItem(R.id.cab_menu_new_chat)
        searchMenuItem?.isVisible = false
        createFolderMenuItem?.isVisible = false
        newChatMenuItem?.isVisible = false
        searchView = searchMenuItem?.actionView as SearchView?
        val searchAutoComplete =
            searchView?.findViewById<AppCompatAutoCompleteTextView>(androidx.appcompat.R.id.search_src_text)

        searchAutoComplete?.hint = getString(R.string.hint_action_search)
        searchView?.findViewById<View>(androidx.appcompat.R.id.search_plate)
            ?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))

        searchView?.setIconifiedByDefault(true)
        searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                isSearchExpanded = true

                if (isSearchMultiselect) {
                    hideTabs(true, if (isCloudVisible) CLOUD_FRAGMENT else INCOMING_FRAGMENT)
                } else {
                    hideTabs(true, CHAT_FRAGMENT)
                    chatExplorer = chatExplorerFragment

                    if (chatExplorer?.isVisible == true) {
                        chatExplorer?.enableSearch(true)
                    }
                }
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                isSearchExpanded = false
                if (isSearchMultiselect) {
                    if (isCloudVisible) {
                        hideTabs(false, CLOUD_FRAGMENT)
                        cDriveExplorer?.closeSearch(collapsedByClick)
                    } else if (isIncomingVisible) {
                        hideTabs(false, INCOMING_FRAGMENT)
                        iSharesExplorer?.closeSearch(collapsedByClick)
                    }
                } else {
                    hideTabs(false, CHAT_FRAGMENT)
                    chatExplorer = chatExplorerFragment

                    if (chatExplorer?.isVisible == true) {
                        chatExplorer?.enableSearch(false)
                    }
                }
                return true
            }
        })

        searchView?.apply {
            maxWidth = Int.MAX_VALUE
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    Timber.d("Query: %s", query)
                    Util.hideKeyboard(this@FileExplorerActivity, 0)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    if (!collapsedByClick) {
                        if (isSearchExpanded)
                            querySearch = newText
                        else
                            clearQuerySearch()
                    } else {
                        collapsedByClick = false
                    }

                    if (isSearchMultiselect) {
                        if (isCloudVisible) {
                            cDriveExplorer?.search(newText)
                        } else if (isIncomingVisible) {
                            iSharesExplorer?.search(newText)
                        }
                    } else {
                        chatExplorer = chatExplorerFragment
                        if (chatExplorer?.isVisible == true) {
                            chatExplorer?.search(newText)
                        }
                    }

                    return true
                }
            })
        }

        if (isSearchMultiselect) {
            isPendingToOpenSearchView()
        }

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Checks if it is pending to open the search view. If so, opens it.
     */
    fun isPendingToOpenSearchView() {
        if (pendingToOpenSearchView && searchMenuItem != null) {
            openSearchView(querySearch)
            pendingToOpenSearchView = false
        }
    }

    private fun openSearchView(search: String?) {
        if (searchMenuItem == null) return
        searchMenuItem?.expandActionView()
        searchView?.setQuery(search, false)
    }

    private fun setCreateFolderVisibility() {
        createFolderMenuItem?.isVisible = intent.action != ACTION_MULTISELECT_FILE
    }

    fun showOrHideSearchMenu(show: Boolean) {
        Timber.d("Showing or hiding search menu $show")
        searchMenuItem?.isVisible = show
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        Timber.d("onPrepareOptionsMenu")

        //Check the tab shown
        if (tabShown != NO_TABS) {
            when (binding.explorerTabsPager.currentItem) {
                CLOUD_TAB -> {
                    setCreateFolderVisibility()
                    newChatMenuItem?.isVisible = false

                    if (isMultiselect) {
                        cDriveExplorer = cloudExplorerFragment
                        searchMenuItem?.isVisible =
                            cDriveExplorer != null && cDriveExplorer?.isFolderEmpty() == false
                    }
                }

                INCOMING_TAB -> {
                    iSharesExplorer = incomingExplorerFragment

                    if (iSharesExplorer != null) {
                        Timber.d("Level deepBrowserTree: %s", deepBrowserTree)

                        if (deepBrowserTree == 0) {
                            createFolderMenuItem?.isVisible = false
                        } else {
                            //Check the folder's permissions
                            iSharesExplorer?.parentHandle?.let { handle ->
                                val n = megaApi.getNodeByHandle(handle)

                                when (megaApi.getAccess(n)) {
                                    MegaShare.ACCESS_OWNER, MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_FULL -> setCreateFolderVisibility()
                                    MegaShare.ACCESS_READ -> createFolderMenuItem?.isVisible = false
                                }
                            }
                        }
                    }

                    newChatMenuItem?.isVisible = false

                    if (isMultiselect) {
                        searchMenuItem?.isVisible = iSharesExplorer?.isFolderEmpty() == false
                    }
                }

                CHAT_TAB -> {
                    chatExplorer = chatExplorerFragment
                    searchMenuItem?.isVisible = chatExplorer != null
                            && chatExplorer?.isListEmpty == false
                    createFolderMenuItem?.isVisible = false
                    newChatMenuItem?.isVisible = false
                }
            }
        } else {
            if (cDriveExplorer != null && !importFileF) {
                setCreateFolderVisibility()
            } else if (importFileF) {
                if (importFragmentSelected != -1) {
                    when (importFragmentSelected) {
                        CLOUD_FRAGMENT -> {
                            createFolderMenuItem?.isVisible = true
                        }

                        INCOMING_FRAGMENT -> {
                            iSharesExplorer = incomingExplorerFragment
                            if (iSharesExplorer != null) {
                                if (deepBrowserTree > 0) {
                                    //Check the folder's permissions
                                    iSharesExplorer?.parentHandle?.let { handle ->
                                        val n = megaApi.getNodeByHandle(handle)

                                        when (megaApi.getAccess(n)) {
                                            MegaShare.ACCESS_OWNER, MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_FULL -> {
                                                createFolderMenuItem?.isVisible = true
                                            }

                                            MegaShare.ACCESS_READ -> {
                                                createFolderMenuItem?.isVisible = false
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        CHAT_FRAGMENT -> {
                            chatExplorer = chatExplorerFragment
                            newChatMenuItem?.isVisible = false
                            searchMenuItem?.isVisible = chatExplorer != null
                                    && chatExplorer?.isListEmpty == false
                        }
                    }
                }
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun setRootTitle() {
        Timber.d("setRootTitle")
        supportActionBar?.setHomeAsUpIndicator(tintIcon(this, R.drawable.ic_close_white))
        supportActionBar?.title = when {
            mode == SELECT -> {
                if (isSelectFile) {
                    if (isMultiselect) {
                        resources.getQuantityString(R.plurals.plural_select_file, 10)
                    } else {
                        resources.getQuantityString(R.plurals.plural_select_file, 1)
                    }
                } else {
                    getString(R.string.title_share_folder_explorer)
                }
            }

            mode == MOVE || mode == COPY || mode == SELECT_CAMERA_FOLDER || mode == IMPORT || mode == ALBUM_IMPORT -> {
                getString(R.string.title_share_folder_explorer)
            }

            mode == UPLOAD && !importFileF -> {
                getString(R.string.title_file_explorer_send_link)
            }

            mode == SAVE -> getString(R.string.title_upload_explorer)

            mode == UPLOAD -> {
                when (importFragmentSelected) {
                    CHAT_FRAGMENT -> getString(R.string.title_chat_explorer)
                    CLOUD_FRAGMENT, IMPORT_FRAGMENT -> getString(R.string.title_upload_explorer)
                    else -> null
                }
            }

            else -> null
        }
    }

    /**
     * Updates screen title.
     */
    fun changeTitle() {
        Timber.d("changeTitle")
        cDriveExplorer = cloudExplorerFragment
        iSharesExplorer = incomingExplorerFragment

        if (tabShown == NO_TABS || mTabsAdapterExplorer == null) {
            if (importFileF) {
                if (importFragmentSelected != -1) {
                    when (importFragmentSelected) {
                        CLOUD_FRAGMENT -> {
                            if (cDriveExplorer != null) {
                                if (cDriveExplorer?.parentHandle == INVALID_HANDLE
                                    || cDriveExplorer?.parentHandle == megaApi.rootNode?.handle
                                ) {
                                    setRootTitle()
                                    supportActionBar?.setSubtitle(R.string.general_select_to_download)
                                } else {
                                    setToolbarTitleByNodeHandle(
                                        cDriveExplorer?.parentHandle ?: return
                                    )
                                }
                            }
                        }

                        CHAT_FRAGMENT, IMPORT_FRAGMENT -> setRootTitle()
                    }
                }
            } else {
                if (cDriveExplorer != null) {
                    if (cDriveExplorer?.parentHandle == -1L || cDriveExplorer?.parentHandle == megaApi.rootNode?.handle) {
                        setRootTitle()
                    } else {
                        setToolbarTitleByNodeHandle(
                            cDriveExplorer?.parentHandle ?: return
                        )
                    }
                }

                showFabButton(false)
            }
        } else {
            val position = binding.explorerTabsPager.currentItem
            val f = mTabsAdapterExplorer?.getFragment(position)

            if (position == 0) {
                if (f is ChatExplorerFragment) {
                    if (tabShown != NO_TABS) {
                        tabShown = CHAT_TAB
                    }

                    supportActionBar?.title = getString(R.string.title_file_explorer_send_link)
                } else if (f is CloudDriveExplorerFragment) {
                    if (tabShown != NO_TABS) {
                        tabShown = CLOUD_TAB
                    }

                    if (f.parentHandle == -1L || f.parentHandle == megaApi.rootNode?.handle) {
                        setRootTitle()
                    } else {
                        setToolbarTitleByNodeHandle(f.parentHandle)
                    }

                    showFabButton(false)
                }
            } else if (position == 1) {
                if (f is IncomingSharesExplorerFragment) {
                    if (tabShown != NO_TABS) {
                        tabShown = INCOMING_TAB
                    }

                    if (deepBrowserTree == 0) {
                        setRootTitle()
                    } else {
                        setToolbarTitleByNodeHandle(f.parentHandle)
                    }
                } else if (f is CloudDriveExplorerFragment) {
                    if (tabShown != NO_TABS) {
                        tabShown = CLOUD_TAB
                    }

                    if (f.parentHandle == -1L || f.parentHandle == megaApi.rootNode?.handle) {
                        setRootTitle()
                    } else {
                        setToolbarTitleByNodeHandle(f.parentHandle)
                    }
                }
                showFabButton(false)
            } else if (position == 2) {
                if (f is ChatExplorerFragment) {
                    if (tabShown != NO_TABS) {
                        tabShown = CHAT_TAB
                    }

                    supportActionBar?.title = getString(R.string.title_chat_explorer)
                } else if (f is IncomingSharesExplorerFragment) {
                    if (tabShown != NO_TABS) {
                        tabShown = INCOMING_TAB
                    }

                    if (deepBrowserTree == 0) {
                        setRootTitle()
                    } else {
                        setToolbarTitleByNodeHandle(f.parentHandle)
                    }

                    showFabButton(false)
                }
            }
        }

        invalidateOptionsMenu()
    }

    private fun setToolbarTitle(title: String?) {
        supportActionBar?.title = title
        supportActionBar?.setHomeAsUpIndicator(tintIcon(this, R.drawable.ic_arrow_back_white))
    }

    private fun setToolbarTitleByNodeHandle(handle: Long) {
        lifecycleScope.launch(ioDispatcher) {
            runCatching {
                megaApi.getNodeByHandle(handle)
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    setToolbarTitle(it?.name ?: "")
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Timber.d("onSaveInstanceState")
        super.onSaveInstanceState(outState)

        with(outState) {
            putBoolean("folderSelected", folderSelected)
            cDriveExplorer = cloudExplorerFragment
            parentHandleCloud = cDriveExplorer?.parentHandle ?: INVALID_HANDLE
            putLong("parentHandleCloud", parentHandleCloud)
            iSharesExplorer = incomingExplorerFragment
            parentHandleIncoming = iSharesExplorer?.parentHandle ?: INVALID_HANDLE
            putLong("parentHandleIncoming", parentHandleIncoming)
            putInt("deepBrowserTree", deepBrowserTree)
            Timber.d("IN BUNDLE -> deepBrowserTree: %s", deepBrowserTree)
            putBoolean("importFileF", importFileF)
            putInt("importFragmentSelected", importFragmentSelected)
            putString("action", action)
            chatExplorerFragment?.let {
                supportFragmentManager.putFragment(this, "chatExplorerFragment", it)
            }
            putString("querySearch", querySearch)
            putBoolean("isSearchExpanded", isSearchExpanded)
            putInt("pendingToAttach", pendingToAttach)
            putInt("totalAttached", totalAttached)
            putInt("totalErrors", totalErrors)
            putBoolean(SHOULD_RESTART_SEARCH, shouldRestartSearch)
            putString(QUERY_AFTER_SEARCH, queryAfterSearch)
            putString(CURRENT_ACTION, currentAction)
            newFolderDialog.checkNewFolderDialogState(this)
        }
    }

    private fun performImportFileBack() {
        if (importFileF) {
            if (isFromUploadDestinationActivity) {
                finishAndRemoveTask()
            } else {
                chooseFragment(IMPORT_FRAGMENT)
            }
        } else {
            viewModel.handleBackNavigation()
        }
    }

    private val isCloudVisible: Boolean
        get() {
            val isImportingToCloud =
                importFileF && importFragmentSelected == CLOUD_FRAGMENT && binding.explorerTabsPager.currentItem == CLOUD_TAB
            return (cDriveExplorer != null && cDriveExplorer?.isVisible == true
                    && (tabShown == CLOUD_TAB && !importFileF || tabShown == NO_TABS || isImportingToCloud))
        }

    private val isIncomingVisible: Boolean
        get() {
            val isImportingToIncoming =
                importFileF && importFragmentSelected == CLOUD_FRAGMENT && binding.explorerTabsPager.currentItem == INCOMING_TAB
            return (iSharesExplorer != null && iSharesExplorer?.isVisible == true
                    && (tabShown == INCOMING_TAB && !importFileF || isImportingToIncoming))
        }

    /**
     * Checks if should start ChatUploadService to share the content or only attach it.
     * If the ChatUploadService has to start, it also checks if the content is already
     * available on Cloud to avoid start upload existing files.
     */
    private fun startChatUploadService() {
        if (chatListItems.isEmpty()) {
            Timber.w("ERROR null chats to upload")
            openManagerAndFinish()
            return
        }

        val filePreparedDocuments = viewModel.getDocuments()

        Timber.d("Launch chat upload with files %s", filePreparedDocuments.size)
        val notEmptyAttachedNodes = attachNodes.isNotEmpty()
        val notEmptyUploadInfo = uploadDocuments.isNotEmpty()
        filesChecked = 0

        if (notEmptyAttachedNodes && !notEmptyUploadInfo) {
            // All files exists, not necessary start ChatUploadService
            pendingToAttach = attachNodes.size * chatListItems.size

            for (node in attachNodes) {
                for (item in chatListItems) {
                    megaChatApi.attachNode(item.chatId, node.handle, this)
                }
            }

            return
        }

        val documentsToShare = if (notEmptyUploadInfo) uploadDocuments else filePreparedDocuments
        val chatIds = chatListItems.map { it.chatId }
        val nodeHandles = attachNodes.map { it.handle }
        val nodeIds = nodeHandles.map { NodeId(it) }
        checkNotificationsPermission(this)
        viewModel.uploadFilesToChat(
            chatIds, documentsToShare ?: emptyList(), nodeIds,
            toDoAfter = {
                openManagerAndFinish()
            }
        )
    }

    private fun openManagerAndFinish() {
        val intent = Intent(this, ManagerActivity::class.java).apply {
            if (isFromUploadDestinationActivity) {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        startActivity(intent)
        finish()
    }

    private fun finishFileExplorer() {
        dismissAlertDialogIfExists(statusDialog)
        Timber.d("finish!!!")
        finishAndRemoveTask()
    }

    /**
     * Checks if files exists in MEGA.
     */
    fun checkIfFilesExistsInMEGA() {
        val filePreparedDocuments = viewModel.getDocuments()
        for (info in filePreparedDocuments) {
            val fingerprint = megaApi.getFingerprint(info.uri.value)
            val node = megaApi.getNodeByFingerprint(fingerprint)
            if (node != null) {
                if (node.parentHandle == myChatFilesNode?.handle) {
                    //	File is in My Chat Files --> Add to attach
                    attachNodes.add(node)
                    filesChecked++
                } else {
                    // File is already in Cloud --> Copy in My Chat Files
                    // Note: This block is executed when a file is first uploaded to a cloud drive,
                    //       and then sent to chat again via the Share Intent from another app.
                    lifecycleScope.launch {
                        val newParentNodeHandle = myChatFilesNode?.handle
                        runCatching {
                            requireNotNull(newParentNodeHandle)
                            copyNodeUseCase(
                                nodeToCopy = NodeId(node.handle),
                                newNodeParent = NodeId(newParentNodeHandle),
                                newNodeName = null
                            )
                        }.onSuccess {
                            filesChecked++
                            attachNodes.add(node)
                            if (filesChecked == filePreparedDocuments.size) {
                                startChatUploadService()
                            }
                        }.onFailure { throwable ->
                            filesChecked++
                            Timber.w("Error copying node into My Chat Files")
                            if (filesChecked == filePreparedDocuments.size) {
                                startChatUploadService()
                            }
                            manageCopyMoveException(throwable)
                        }
                    }
                }
            } else {
                uploadDocuments.add(info)
                filesChecked++
            }
        }

        if (filesChecked == filePreparedDocuments.size) {
            startChatUploadService()
        }
    }

    /**
     * Handle processed upload intent.
     *
     * @param infos List<ShareInfo> containing all the upload info.
     */
    private fun onIntentProcessed(infos: List<DocumentEntity>?) {
        Timber.d("onIntentChatProcessed")
        if (intent != null && intent.action !== ACTION_PROCESSED) {
            intent.action = ACTION_PROCESSED
        }

        when {
            infos == null -> {
                dismissAlertDialogIfExists(statusDialog)
                showSnackbar(getString(R.string.upload_can_not_open))
            }

            existsMyChatFilesFolder() -> {
                setMyChatFilesFolder(myChatFilesFolder)
                checkIfFilesExistsInMEGA()
            }

            else -> {
                megaApi.getMyChatFilesFolder(GetAttrUserListener(this))
            }
        }
    }

    private fun onIntentProcessed() {
        val documents = viewModel.getDocuments()

        if (intent != null && intent.action !== ACTION_PROCESSED) {
            intent.action = ACTION_PROCESSED
        }

        Timber.d("intent processed!")

        if (folderSelected) {
            if (documents.isEmpty()) {
                dismissAlertDialogIfExists(statusDialog)
                showSnackbar(getString(R.string.upload_can_not_open))
                return
            }

            if (viewModel.storageState === StorageState.PayWall) {
                dismissAlertDialogIfExists(statusDialog)
                showOverDiskQuotaPaywallWarning()
                return
            }

            parentHandle = if (tabShown == INCOMING_TAB) {
                iSharesExplorer?.parentHandle ?: parentHandleCloud
            } else {
                cDriveExplorer?.parentHandle ?: parentHandleCloud
            }

            var parentNode = megaApi.getNodeByHandle(parentHandle)
            if (parentNode == null) {
                parentNode = megaApi.rootNode
                parentHandle = parentNode?.handle ?: INVALID_HANDLE
            }

            lifecycleScope.launch {
                runCatching {
                    checkFileNameCollisionsUseCase(
                        files = documents,
                        parentNodeId = NodeId(parentHandle)
                    )
                }.onSuccess { collisions ->
                    dismissAlertDialogIfExists(statusDialog)
                    if (collisions.isNotEmpty()) {
                        viewModel.setIsAskingForCollisionsResolution(isAskingForCollisionsResolution = true)
                        nameCollisionActivityLauncher.launch(ArrayList(collisions))
                    }
                    collisions.map { it.path.value }.let { collidedPaths ->
                        if (collidedPaths.size < documents.size) {
                            viewModel.uploadFiles(parentHandle, collidedPaths)
                        }
                    }
                }.onFailure {
                    dismissAlertDialogIfExists(statusDialog)
                    Util.showErrorAlertDialog(
                        getString(R.string.error_temporary_unavaible),
                        false,
                        this@FileExplorerActivity
                    )
                }
            }
        }
    }

    /**
     * Confirms the action after selecting some nodes.
     *
     * @param handles Array of selected node handles.
     */
    fun buttonClick(handles: LongArray) {
        Timber.d("handles: %s", handles.size)
        val intent = Intent()
        intent.putExtra(Constants.NODE_HANDLES, handles)
        intent.putStringArrayListExtra(Constants.SELECTED_CONTACTS, selectedContacts)
        setResult(RESULT_OK, intent)
        finishAndRemoveTask()
    }

    /**
     * Method to create and show a progress dialog
     *
     * @param cancelable Flag to set if the progress dialog is cancelable or not
     * @param message    Message to display into the progress dialog
     */
    private fun createAndShowProgressDialog(cancelable: Boolean, message: String) {
        statusDialog?.dismiss()
        val temp: AlertDialog
        try {
            temp = createProgressDialog(this, message)
            temp.setCancelable(cancelable)
            temp.setCanceledOnTouchOutside(cancelable)
            temp.show()
        } catch (e: Exception) {
            Timber.w(e, "Error creating and showing progress dialog.")
            return
        }

        statusDialog = temp
    }

    /**
     * Confirms the action after selection a node.
     *
     * @param handle Handle of the selected node.
     */
    fun buttonClick(handle: Long) {
        Timber.d("handle: %s", handle)
        if (tabShown == INCOMING_TAB) {
            if (deepBrowserTree == 0) {
                val intent = Intent()
                setResult(RESULT_FIRST_USER, intent)
                finishAndRemoveTask()
                return
            }
        }

        folderSelected = true
        parentHandleCloud = handle

        when (mode) {
            MOVE -> {
                val parentNode = megaApi.getNodeByHandle(handle) ?: megaApi.rootNode
                val intent = Intent()
                intent.putExtra("MOVE_TO", parentNode?.handle)
                intent.putExtra("MOVE_HANDLES", moveFromHandles)
                setResult(RESULT_OK, intent)
                Timber.d("finish!")
                finishAndRemoveTask()
            }

            COPY -> {
                val parentNode = megaApi.getNodeByHandle(handle) ?: megaApi.rootNode
                val intent = Intent()
                intent.putExtra("COPY_TO", parentNode?.handle)
                intent.putExtra("COPY_HANDLES", copyFromHandles)
                setResult(RESULT_OK, intent)
                Timber.d("finish!")
                finishAndRemoveTask()
            }

            UPLOAD, SAVE -> {
                Timber.d("mode UPLOAD")
                if (intent.action == ACTION_SAVE_TO_CLOUD) {
                    logDocumentScanEvent(isCloudDrive = true)
                }
                if (viewModel.isImportingText(intent)) {
                    val parentNode = megaApi.getNodeByHandle(handle) ?: megaApi.rootNode
                    val info = viewModel.textInfoContent

                    if (info != null) {
                        val name =
                            viewModel.uiState.value.namesByOriginalName[info.subject]
                                ?: info.subject
                        createFile(name, info.fileContent, parentNode, info.isUrl)
                    }

                    return
                }

                if (viewModel.getDocuments().isEmpty()) {
                    viewModel.ownFilePrepareTask(this, intent)
                    createAndShowProgressDialog(
                        false,
                        resources.getQuantityString(R.plurals.upload_prepare, 1)
                    )
                } else {
                    onIntentProcessed()
                }
            }

            IMPORT -> {
                val parentNode = megaApi.getNodeByHandle(handle) ?: megaApi.rootNode

                if (tabShown == CLOUD_TAB) {
                    fragmentHandle = megaApi.rootNode?.handle ?: INVALID_HANDLE
                } else if (tabShown == INCOMING_TAB) {
                    fragmentHandle = -1
                }

                val intent = Intent()
                intent.putExtra(INTENT_EXTRA_KEY_IMPORT_TO, parentNode?.handle)
                intent.putExtra("fragmentH", fragmentHandle)

                if (importChatHandles != null) {
                    intent.putExtra("HANDLES_IMPORT_CHAT", importChatHandles)
                }

                setResult(RESULT_OK, intent)
                Timber.d("finish!")
                finishAndRemoveTask()
            }

            ALBUM_IMPORT -> {
                val parentNode = megaApi.getNodeByHandle(handle) ?: megaApi.rootNode

                val intent = Intent()
                intent.putExtra(INTENT_EXTRA_KEY_IMPORT_TO, parentNode?.handle)
                setResult(RESULT_OK, intent)

                finishAndRemoveTask()
            }

            SELECT -> {
                if (isSelectFile) {
                    val intent = Intent()
                    intent.putExtra(EXTRA_SELECTED_FOLDER, handle)
                    intent.putStringArrayListExtra(Constants.SELECTED_CONTACTS, selectedContacts)
                    setResult(RESULT_OK, intent)
                    finishAndRemoveTask()
                } else {
                    val parentNode = megaApi.getNodeByHandle(handle) ?: megaApi.rootNode
                    val intent = Intent()
                    intent.putExtra(EXTRA_SELECTED_FOLDER, parentNode?.handle)
                    intent.putStringArrayListExtra(Constants.SELECTED_CONTACTS, selectedContacts)
                    setResult(RESULT_OK, intent)
                    finishAndRemoveTask()
                }
            }

            SELECT_CAMERA_FOLDER -> {
                val parentNode = megaApi.getNodeByHandle(handle) ?: megaApi.rootNode
                if (parentNode?.handle != -1L) {
                    Timber.d("Successfully selected the new Cloud Drive Folder")
                } else {
                    Timber.e("The new Cloud Drive Folder is invalid")
                }
                val intent = Intent()
                intent.putExtra(EXTRA_MEGA_SELECTED_FOLDER, parentNode?.handle)
                setResult(RESULT_OK, intent)
                finishAndRemoveTask()
            }
        }
    }

    private fun logDocumentScanEvent(isCloudDrive: Boolean) {
        ScanFileType.entries.getOrNull(intent.getIntExtra(EXTRA_SCAN_FILE_TYPE, 0))?.let { type ->
            when {
                type == ScanFileType.Pdf && isCloudDrive ->
                    Analytics.tracker.trackEvent(DocumentScannerUploadingPDFToCloudDriveEvent)

                type == ScanFileType.Pdf && !isCloudDrive ->
                    Analytics.tracker.trackEvent(DocumentScannerUploadingPDFToChatEvent)

                type == ScanFileType.Jpg && isCloudDrive ->
                    Analytics.tracker.trackEvent(DocumentScannerUploadingImageToCloudDriveEvent)

                type == ScanFileType.Jpg && !isCloudDrive ->
                    Analytics.tracker.trackEvent(DocumentScannerUploadingImageToChatEvent)
            }
        }
    }

    /**
     * Goes back to Cloud.
     *
     * @param handle        Parent handle of the folder to open.
     * @param message       Message to show.
     */
    private fun backToCloud(handle: Long, message: String?) = lifecycleScope.launch {
        Timber.d("handle: %s", handle)
        val isSingleActivity = getFeatureFlagValueUseCase(AppFeatures.SingleActivity)
        if (isSingleActivity) {
            finish()
            return@launch
        }

        val startIntent = Intent(this@FileExplorerActivity, ManagerActivity::class.java).apply {
            if (isFromUploadDestinationActivity) {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        if (handle != INVALID_HANDLE) {
            startIntent.action = Constants.ACTION_OPEN_FOLDER
            startIntent.putExtra(Constants.INTENT_EXTRA_KEY_PARENT_HANDLE, handle)
        }

        startIntent.putExtra(Constants.EXTRA_MESSAGE, message)

        startActivity(startIntent)
        finish()
    }

    /**
     * Shows a Snackbar.
     *
     * @param s Text to show.
     */
    fun showSnackbar(s: String?) {
        showSnackbar(binding.fragmentContainerFileExplorer, s ?: return)
    }

    /**
     * Creates a file.
     *
     * @param name       File name.
     * @param data       Content of the file.
     * @param parentNode Parent node in which the file will be created.
     * @param isURL      True if it is a shared URL, false if it is only plain text.
     */
    fun createFile(name: String?, data: String?, parentNode: MegaNode?, isURL: Boolean) {
        if (viewModel.storageState === StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            return
        }

        val file: File? = if (isURL) {
            FileUtil.createTemporalURLFile(this, name, data)
        } else {
            FileUtil.createTemporalTextFile(this, name, data)
        }

        if (file == null) {
            showSnackbar(getString(R.string.general_text_error))
            return
        }

        parentHandle = parentNode?.handle ?: megaApi.rootNode?.handle ?: INVALID_HANDLE
        lifecycleScope.launch {
            runCatching {
                checkFileNameCollisionsUseCase(
                    files = listOf(file.let {
                        DocumentEntity(
                            name = it.name,
                            size = it.length(),
                            lastModified = it.lastModified(),
                            uri = UriPath(it.toUri().toString()),
                        )
                    }),
                    parentNodeId = NodeId(parentHandle)
                )
            }.onSuccess { collisions ->
                collisions.firstOrNull()?.let {
                    nameCollisionActivityLauncher.launch(arrayListOf(it))
                } ?: viewModel.uploadFile(file, parentHandle)
            }.onFailure {
                Timber.e(it, "Cannot check name collisions")
                showSnackbar(getString(R.string.general_text_error))
            }
        }
    }

    override fun finishRenameActionWithSuccess(newName: String) {
        //No action needed
    }

    override fun actionConfirmed() {
        //No update needed
    }

    /**
     * Get current parent node.
     *
     * @return  The current parent node.
     */
    private val currentParentNode: MegaNode?
        get() {
            cDriveExplorer = cloudExplorerFragment
            iSharesExplorer = incomingExplorerFragment

            parentHandle = when {
                isCloudVisible -> cDriveExplorer?.parentHandle ?: INVALID_HANDLE
                isIncomingVisible -> iSharesExplorer?.parentHandle ?: INVALID_HANDLE
                else -> INVALID_HANDLE
            }

            return megaApi.getNodeByHandle(parentHandle)
        }

    override fun createFolder(folderName: String) {
        Timber.d("createFolder")

        if (!Util.isOnline(this)) {
            showSnackbar(getString(R.string.error_server_connection_problem))
            return
        }

        if (isFinishing) {
            return
        }

        val parentHandle: Long = -1
        var parentNode: MegaNode? = currentParentNode

        if (parentNode != null) {
            Timber.d("parentNode != null: %s", parentNode.name)
            var exists = false
            val nL = megaApi.getChildren(parentNode)
            for (i in nL.indices) {
                if (folderName.compareTo(nL[i].name) == 0) {
                    exists = true
                }
            }

            if (!exists) {
                statusDialog = null

                try {
                    statusDialog =
                        createProgressDialog(this, getString(R.string.context_creating_folder))
                    statusDialog?.show()
                } catch (e: Exception) {
                    return
                }

                megaApi.createFolder(folderName, parentNode, CreateFolderListener(this))
            } else {
                showSnackbar(getString(R.string.context_folder_already_exists))
            }
        } else {
            Timber.w("parentNode == null: %s", parentHandle)
            parentNode = megaApi.rootNode

            if (parentNode != null) {
                Timber.d("megaApi.getRootNode() != null")
                var exists = false
                val nL = megaApi.getChildren(parentNode)
                for (i in nL.indices) {
                    if (folderName.compareTo(nL[i].name) == 0) {
                        exists = true
                    }
                }

                if (!exists) {
                    statusDialog = null
                    try {
                        statusDialog =
                            createProgressDialog(this, getString(R.string.context_creating_folder))
                        statusDialog?.show()
                    } catch (e: Exception) {
                        return
                    }
                    megaApi.createFolder(folderName, parentNode, CreateFolderListener(this))
                } else {
                    showSnackbar(getString(R.string.context_folder_already_exists))
                }
            }
        }
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart")
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, error: MegaError) {
        Timber.d("onRequestFinish")
        if (request.type == MegaRequest.TYPE_LOGIN) {
            if (error.errorCode != MegaError.API_OK) {
                Timber.w("Login failed with error code: %s", error.errorCode)
                runCatching { loginMutex.unlock() }
                    .onFailure { Timber.w("Exception unlocking login mutex $it") }
            } else {
                with(binding) {
                    fileLoginProgressBar.isVisible = true
                    fileLoginFetchingNodesBar.isVisible = false
                    fileLoginLoggingInText.isVisible = true
                    fileLoginFetchNodesText.isVisible = true
                    fileLoginPrepareNodesText.isVisible = false
                }
                lifecycleScope.launch {
                    runCatching {
                        saveAccountCredentialsUseCase()
                    }.onFailure {
                        Timber.e(it)
                    }
                }
                Timber.d("Logged in with session")
                Timber.d("Setting account auth token for folder links.")
                megaApiFolder.accountAuth = megaApi.accountAuth
                megaApi.fetchNodes(this)

                // Get cookies settings after login.
                getInstance().checkEnabledCookies()
            }
        } else if (request.type == MegaRequest.TYPE_FETCH_NODES) {
            if (error.errorCode == MegaError.API_OK) {
                lifecycleScope.launch {
                    runCatching {
                        saveAccountCredentialsUseCase()
                    }.onFailure {
                        Timber.e(it)
                    }
                }
                binding.fileLoggingInLayout.isVisible = false
                afterLoginAndFetch()
                runCatching { loginMutex.unlock() }
                    .onFailure { Timber.w("Exception unlocking login mutex $it") }
            }
        }
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava, request: MegaRequest,
        e: MegaError,
    ) {

    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {

    }

    override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser>?) {

    }

    override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert>?) {
        Timber.d("onUserAlertsUpdate")
    }

    override fun onEvent(api: MegaApiJava, event: MegaEvent?) {}

    override fun onSetsUpdate(api: MegaApiJava, sets: ArrayList<MegaSet>?) {
    }

    override fun onSetElementsUpdate(
        api: MegaApiJava,
        elements: ArrayList<MegaSetElement>?,
    ) {
    }

    override fun onGlobalSyncStateChanged(api: MegaApiJava) {}

    override fun onNodesUpdate(api: MegaApiJava, updatedNodes: ArrayList<MegaNode>?) {
        Timber.d("onNodesUpdate")
        cDriveExplorer?.let { cDriveExplorer ->
            if (cloudExplorerFragment != null) {
                cDriveExplorer.lifecycleScope.launch {
                    nodes = withContext(ioDispatcher) {
                        if (megaApi.getNodeByHandle(cDriveExplorer.parentHandle) != null) {
                            megaApi.getChildren(megaApi.getNodeByHandle(cDriveExplorer.parentHandle))
                        } else {
                            megaApi.rootNode?.let { rootNode ->
                                parentHandle = rootNode.handle ?: INVALID_HANDLE
                                megaApi.getChildren(megaApi.getNodeByHandle(cDriveExplorer.parentHandle))
                            }
                        }
                    }

                    nodes?.let {
                        cDriveExplorer.updateNodesByAdapter(it)
                    }
                    cDriveExplorer.recyclerView.invalidate()
                }
            }
        }
    }

    public override fun onDestroy() {
        megaApi.removeGlobalListener(this)
        megaApi.removeRequestListener(this)
        megaChatApi.removeChatRequestListener(this)
        dismissAlertDialogIfExists(statusDialog)
        dismissAlertDialogIfExists(newFolderDialog)
        super.onDestroy()
    }

    override fun onAccountUpdate(api: MegaApiJava) {

    }

    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest>?,
    ) {

    }

    /**
     * Sets action bar subtitle.
     *
     * @param s Text to set.
     */
    fun setToolbarSubtitle(s: String?) {
        supportActionBar?.subtitle = s
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }

            R.id.cab_menu_create_folder -> {
                newFolderDialog = showNewFolderDialog(
                    this,
                    this,
                    currentParentNode,
                    null
                )
            }

            R.id.cab_menu_new_chat -> {
                if (megaApi.rootNode != null) {
                    val contacts = megaApi.contacts
                    if (contacts == null) {
                        showSnackbar(getString(R.string.no_contacts_invite))
                    } else {
                        if (contacts.isEmpty()) {
                            showSnackbar(getString(R.string.no_contacts_invite))
                        } else {
                            createChatLauncher.launch(
                                Intent(this, AddContactActivity::class.java)
                                    .putExtra(EXTRA_CONTACT_TYPE, CONTACT_TYPE_MEGA)
                            )
                        }
                    }
                } else {
                    Timber.w("Online but not megaApi")
                    Util.showErrorAlertDialog(
                        getString(R.string.error_server_connection_problem),
                        false,
                        this
                    )
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Finish the create chat request.
     *
     * @param errorCode Code with which the request finished.
     */
    fun onRequestFinishCreateChat(errorCode: Int) {
        Timber.d("onRequestFinishCreateChat")
        if (errorCode == MegaChatError.ERROR_OK) {
            Timber.d("Chat CREATED.")
            //Update chat view
            chatExplorer = chatExplorerFragment
            chatExplorer?.setChats()
            showSnackbar(getString(R.string.new_group_chat_created))
        } else {
            Timber.w("ERROR WHEN CREATING CHAT %s", errorCode)
            showSnackbar(getString(R.string.create_chat_error))
        }
    }

    private fun startOneToOneChat(user: MegaUser) {
        Timber.d("User: %s", user.handle)
        val chat = megaChatApi.getChatRoomByUser(user.handle)
        val peers = MegaChatPeerList.createInstance()

        if (chat == null) {
            Timber.d("No chat, create it!")
            peers.addPeer(user.handle, MegaChatPeerList.PRIV_STANDARD)
            megaChatApi.createChat(false, peers, this)
        } else {
            Timber.d("There is already a chat, open it!")
            showSnackbar(getString(R.string.chat_already_exists))
        }
    }

    private fun getChatAdded(listItems: ArrayList<ChatExplorerListItem>) {
        val chats = ArrayList<MegaChatRoom>()
        val users = ArrayList<User>()
        createAndShowProgressDialog(true, getString(R.string.preparing_chats))

        for (item in listItems) {
            if (item.chat != null) {
                megaChatApi.getChatRoom(item.chat.chatId)?.let {
                    chats.add(it)
                }
            } else if (item.contactItem?.user != null) {
                users.add(item.contactItem.user)
            }
        }
        if (users.isNotEmpty()) {
            val listener = CreateChatListener(
                action = CreateChatListener.SEND_FILE_EXPLORER_CONTENT,
                chats = chats,
                usersNoChatSize = users.size,
                context = this,
                snackbarShower = this
            ) { resultChats: List<MegaChatRoom> -> sendToChats(resultChats) }

            for (user in users) {
                val peers = MegaChatPeerList.createInstance()
                peers.addPeer(user.handle, MegaChatPeerList.PRIV_STANDARD)
                megaChatApi.createChat(false, peers, listener)
            }
        } else {
            sendToChats(chats)
        }
    }

    override fun onClick(v: View) {
        Timber.d("onClick")

        when (v.id) {
            R.id.fab_file_explorer -> {
                if (intent.action == ACTION_UPLOAD_TO_CHAT) {
                    logDocumentScanEvent(isCloudDrive = false)
                }
                v.isEnabled = false
                chatExplorer = chatExplorerFragment
                chatExplorer?.let { getChatAdded(it.addedChats ?: return) }
            }

            R.id.new_group_button -> {
                if (megaApi.rootNode != null) {
                    val contacts = megaApi.contacts
                    if (contacts == null) {
                        showSnackbar(getString(R.string.no_contacts_invite))
                    } else {
                        if (contacts.isEmpty()) {
                            showSnackbar(getString(R.string.no_contacts_invite))
                        } else {
                            createChatLauncher.launch(
                                Intent(this, AddContactActivity::class.java)
                                    .putExtra(EXTRA_CONTACT_TYPE, CONTACT_TYPE_MEGA)
                                    .putExtra(EXTRA_ONLY_CREATE_GROUP, true)
                            )
                        }
                    }
                } else {
                    Timber.w("Online but not megaApi")
                    Util.showErrorAlertDialog(
                        getString(R.string.error_server_connection_problem),
                        false,
                        this
                    )
                }
            }
        }
    }

    private fun sendToChats(chats: List<MegaChatRoom>) {
        dismissAlertDialogIfExists(statusDialog)
        chatListItems.addAll(chats)

        if (viewModel.isImportingText(intent)) {
            Timber.d("Handle intent of text plain")
            val message = viewModel.messageToShare
            if (message != null) {
                for (i in chatListItems.indices) {
                    megaChatApi.sendMessage(chatListItems[i].chatId, message)
                }

                if (chatListItems.size == 1) {
                    val chatItem = chatListItems[0]
                    val idChat = chatItem.chatId
                    val intent = Intent(this, ManagerActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent.action = Constants.ACTION_CHAT_NOTIFICATION_MESSAGE
                    intent.putExtra(Constants.CHAT_ID, idChat)
                    startActivity(intent)
                } else {
                    val chatIntent = Intent(this, ManagerActivity::class.java)
                    chatIntent.action = Constants.ACTION_CHAT_SUMMARY
                    startActivity(chatIntent)
                }
            }

            return
        }

        val filePreparedDocuments = viewModel.getDocuments()

        if (filePreparedDocuments.isEmpty()) {
            createAndShowProgressDialog(
                false,
                resources.getQuantityString(R.plurals.upload_prepare, 1)
            )

            viewModel.ownFilePrepareTask(this, intent)
        } else {
            onIntentProcessed(filePreparedDocuments)
        }
        return
    }

    private fun onChatPresenceLastGreen(userhandle: Long, lastGreen: Int) {
        val state = megaChatApi.getUserOnlineStatus(userhandle)

        if (state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID) {
            val formattedDate = TimeUtils.lastGreenDate(this, lastGreen)

            if (userhandle != megaChatApi.myUserHandle) {
                chatExplorer = chatExplorerFragment
                chatExplorer?.updateLastGreenContact(userhandle, formattedDate)
            }
        }
    }

    private val chatExplorerFragment: ChatExplorerFragment?
        get() {
            if (importFileF) {
                return supportFragmentManager.findFragmentByTag("chatExplorer") as ChatExplorerFragment?
            }
            mTabsAdapterExplorer?.getFragment(2)?.let {
                val c = it as ChatExplorerFragment
                return if (c.isAdded) c else null
            } ?: return null
        }
    private val incomingExplorerFragment: IncomingSharesExplorerFragment?
        get() {
            mTabsAdapterExplorer?.getFragment(1)?.let {
                val iS = it as IncomingSharesExplorerFragment
                return if (iS.isAdded) iS else null
            } ?: return null
        }
    private val cloudExplorerFragment: CloudDriveExplorerFragment?
        get() {
            if (tabShown == NO_TABS) {
                return supportFragmentManager.findFragmentByTag("cDriveExplorer") as CloudDriveExplorerFragment?
            }
            mTabsAdapterExplorer?.getFragment(0)?.let {
                val cD = it as CloudDriveExplorerFragment
                return if (cD.isAdded) cD else null
            } ?: return null
        }

    /**
     * Refresh the order of nodes for cloud explorer.
     *
     * @param order Order to apply.
     */
    fun refreshCloudExplorerOrderNodes(order: Int) {
        cDriveExplorer = cloudExplorerFragment
        cDriveExplorer?.orderNodes(order)
    }

    /**
     * Refresh the order of nodes for incoming explorer.
     *
     * @param order Order to apply.
     */
    fun refreshIncomingExplorerOrderNodes(order: Int) {
        iSharesExplorer = incomingExplorerFragment
        iSharesExplorer?.updateNodesByOrder(order)
    }

    /**
     * Collapses search view.
     */
    fun collapseSearchView() {
        if (searchMenuItem == null) {
            return
        }

        collapsedByClick = true
        searchMenuItem?.collapseActionView()
    }

    /**
     * Gets the parent node to copy.
     *
     * @return The node.
     */
    fun parentMoveCopy(): MegaNode? = parentMoveCopy

    /**
     * Increases the deep browser tree of incoming fragment.
     */
    fun increaseDeepBrowserTree() = deepBrowserTree++

    /**
     * Decreases the deep browser tree of incoming fragment.
     */
    fun decreaseDeepBrowserTree() = deepBrowserTree--

    /**
     * Sets the name files in the view model.
     */
    fun setNameFiles(nameFiles: Map<String, String>) {
        viewModel.setFileNames(nameFiles)
    }

    /**
     * Current item.
     */
    val currentItem: DrawerItem?
        get() {
            if (binding.explorerTabsPager.currentItem == 0) {
                cDriveExplorer = cloudExplorerFragment
                if (cDriveExplorer != null) {
                    return DrawerItem.CLOUD_DRIVE
                }
            } else {
                iSharesExplorer = incomingExplorerFragment
                if (iSharesExplorer != null) {
                    return DrawerItem.SHARED_ITEMS
                }
            }

            return null
        }

    /**
     * Sets a node as "My chat files" folder.
     *
     * @param myChatFilesNode The node to set.
     */
    fun setMyChatFilesFolder(myChatFilesNode: MegaNode?) {
        this.myChatFilesNode = myChatFilesNode
    }

    /**
     * Finish the create folder action.
     *
     * @param success True if the action finished with success, false otherwise.
     * @param handle  Handle of the new folder.
     */
    fun finishCreateFolder(success: Boolean, handle: Long) {
        dismissAlertDialogIfExists(statusDialog)

        if (success) {
            cDriveExplorer = cloudExplorerFragment
            iSharesExplorer = incomingExplorerFragment

            if (isCloudVisible) {
                cDriveExplorer?.navigateToFolder(handle)
                parentHandleCloud = handle
                hideTabs(true, CLOUD_TAB)
            } else if (isIncomingVisible) {
                iSharesExplorer?.navigateToFolder(handle)
                parentHandleIncoming = handle
                hideTabs(true, INCOMING_TAB)
            }
        }
    }

    /**
     * Sets the query search to null.
     */
    fun clearQuerySearch() {
        querySearch = null
    }

    /**
     * Updates the query search.
     */
    fun setQueryAfterSearch() {
        queryAfterSearch = querySearch
    }

    /**
     * Checks if should reopen the search view.
     *
     * @return True if should reopen search view, false otherwise.
     */
    fun shouldReopenSearch(): Boolean {
        if (queryAfterSearch == null) return false

        openSearchView(queryAfterSearch)
        queryAfterSearch = null
        return true
    }

    /**
     * Hides or shows tabs of a section depending on the navigation level
     * and if select mode is enabled or not.
     *
     * @param hide       If true, hides the tabs, else shows them.
     * @param currentTab The current tab where the action happens.
     */
    fun hideTabs(hide: Boolean, currentTab: Int) {
        if (!hide && (queryAfterSearch != null || isSearchExpanded || pendingToOpenSearchView)) {
            return
        }
        when (currentTab) {
            CLOUD_FRAGMENT -> if (cloudExplorerFragment == null
                || !hide && parentHandleCloud != cloudRootHandle && parentHandleCloud != INVALID_HANDLE
            ) {
                return
            }

            INCOMING_FRAGMENT -> if (incomingExplorerFragment == null
                || !hide && parentHandleIncoming != INVALID_HANDLE
            ) {
                return
            }

            CHAT_FRAGMENT -> if (chatExplorerFragment == null) {
                return
            }
        }

        binding.explorerTabsPager.isUserInputEnabled = !hide

        // If no tab should be shown, keep hide.
        binding.slidingTabsFileExplorer.isVisible = !hide && tabShown != NO_TABS
    }

    /**
     * Shows the sort by view.
     */
    fun showSortByPanel() {
        if (bottomSheetDialogFragment.isBottomSheetDialogShown()) {
            return
        }

        bottomSheetDialogFragment =
            if (incomingExplorerFragment != null && deepBrowserTree == 0 && binding.explorerTabsPager.currentItem == INCOMING_TAB) {
                newInstance(Constants.ORDER_OTHERS)
            } else {
                newInstance(Constants.ORDER_CLOUD)
            }

        bottomSheetDialogFragment?.let { it.show(supportFragmentManager, it.tag) }
    }

    /**
     * Receive changes to OnChatPresenceLastGreen and make the necessary changes
     */
    fun checkChatChanges() {
        collectFlow(monitorChatPresenceLastGreenUpdatesUseCase()) {
            onChatPresenceLastGreen(it.handle, it.lastGreen)
        }
    }

    private fun addStartUploadTransferView() {
        binding.root.addView(
            createStartTransferView(
                activity = this,
                transferEventState = viewModel.uiState.map { it.uploadEvent },
                onConsumeEvent = viewModel::consumeUploadEvent,
            ) { startTransferEvent ->
                if (viewModel.isAskingForCollisionsResolution()) {
                    //Not ready to finish yet
                    return@createStartTransferView
                }

                ((startTransferEvent as StartTransferEvent.FinishUploadProcessing).triggerEvent as TransferTriggerEvent.StartUpload.Files).let {
                    backToCloud(it.destinationId.longValue, null)
                }
            }
        )
    }

    internal fun setCurrentTab(position: Int) {
        val itemCount = binding.explorerTabsPager.adapter?.itemCount ?: 0
        if (position in 0 until itemCount) {
            binding.explorerTabsPager.setCurrentItem(position, false)
        }
    }

    companion object {
        private const val SHOULD_RESTART_SEARCH = "SHOULD_RESTART_SEARCH"
        private const val QUERY_AFTER_SEARCH = "QUERY_AFTER_SEARCH"
        private const val CURRENT_ACTION = "CURRENT_ACTION"

        /**
         * Cloud fragment.
         */
        const val CLOUD_FRAGMENT = 0

        /**
         * Incoming fragment.
         */
        const val INCOMING_FRAGMENT = 1

        /**
         * Chats fragment.
         */
        const val CHAT_FRAGMENT = 3

        /**
         * Import fragment.
         */
        const val IMPORT_FRAGMENT = 4

        /**
         * Intent extra for share flag.
         */
        const val EXTRA_FROM_SHARE = "from_share"

        /**
         * Intent extra for parent handle.
         */
        const val EXTRA_PARENT_HANDLE = "parent_handle"

        /**
         * Intent extra for selected folder.
         */
        const val EXTRA_SELECTED_FOLDER = "selected_folder"

        /**
         * Intent extra for the selected MEGA Folder
         */
        const val EXTRA_MEGA_SELECTED_FOLDER = "EXTRA_MEGA_SELECTED_FOLDER"

        /**
         * Intent extra for the scan file type
         */
        const val EXTRA_SCAN_FILE_TYPE = "scan_file_type"

        /**
         * Intent extra to check whether or not there are multiple scans to be uploaded
         */
        const val EXTRA_HAS_MULTIPLE_SCANS = "EXTRA_HAS_MULTIPLE_SCANS"

        /**
         * Intent action for processed info.
         */
        @JvmField
        var ACTION_PROCESSED = "CreateLink.ACTION_PROCESSED"

        /**
         * Intent action for picking a folder where to move.
         */
        @JvmField
        var ACTION_PICK_MOVE_FOLDER = "ACTION_PICK_MOVE_FOLDER"

        /**
         * Intent action for picking a folder where to copy.
         */
        @JvmField
        var ACTION_PICK_COPY_FOLDER = "ACTION_PICK_COPY_FOLDER"

        /**
         * Intent action for picking a folder where to import.
         */
        @JvmField
        var ACTION_PICK_IMPORT_FOLDER = "ACTION_PICK_IMPORT_FOLDER"

        /**
         * Intent action for selecting a folder to share.
         */
        @JvmField
        var ACTION_SELECT_FOLDER_TO_SHARE = "ACTION_SELECT_FOLDER_TO_SHARE"

        /**
         * Intent action for choosing a folder to sync.
         */
        @JvmField
        var ACTION_CHOOSE_MEGA_FOLDER_SYNC = "ACTION_CHOOSE_MEGA_FOLDER_SYNC"

        /**
         * Intent action for selecting multiple files.
         */
        @JvmField
        var ACTION_MULTISELECT_FILE = "ACTION_MULTISELECT_FILE"

        /**
         * Intent action for uploading to chat.
         */
        @JvmField
        var ACTION_UPLOAD_TO_CHAT = "ACTION_UPLOAD_TO_CHAT"

        /**
         * Intent action for saving to cloud.
         */
        @JvmField
        var ACTION_SAVE_TO_CLOUD = "ACTION_SAVE_TO_CLOUD"

        /**
         * Intent action for importing an album.
         */
        @JvmField
        var ACTION_IMPORT_ALBUM = "ACTION_IMPORT_ALBUM"

        /**
         * Upload mode.
         */
        const val UPLOAD = 0

        /**
         * Move mode.
         */
        const val MOVE = 1

        /**
         * Copy mode.
         */
        const val COPY = 2

        /**
         * Camera mode.
         */
        const val CAMERA = 3

        /**
         * Import mode.
         */
        const val IMPORT = 4

        /**
         * Select mode.
         */
        const val SELECT = 5

        /**
         * Select camera folder mode.
         */
        const val SELECT_CAMERA_FOLDER = 7

        /**
         * Share link mode.
         */
        const val SHARE_LINK = 8

        /**
         * Save mode.
         */
        const val SAVE = 9

        /**
         * Import album mode.
         */
        const val ALBUM_IMPORT = 10

        private const val NO_TABS = -1
        const val CLOUD_TAB = 0
        const val INCOMING_TAB = 1
        private const val CHAT_TAB = 2
        private const val SHOW_TABS = 3
    }
}
