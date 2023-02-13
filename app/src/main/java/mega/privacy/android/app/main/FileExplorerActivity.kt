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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.MegaApplication.Companion.isLoggingIn
import mega.privacy.android.app.R
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_VIEW_MODE
import mega.privacy.android.app.databinding.ActivityFileExplorerBinding
import mega.privacy.android.app.generalusecase.FilePrepareUseCase
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.CreateChatListener
import mega.privacy.android.app.listeners.CreateFolderListener
import mega.privacy.android.app.listeners.GetAttrUserListener
import mega.privacy.android.app.main.AddContactActivity.ALLOW_ADD_PARTICIPANTS
import mega.privacy.android.app.main.AddContactActivity.EXTRA_CHAT_LINK
import mega.privacy.android.app.main.AddContactActivity.EXTRA_CHAT_TITLE
import mega.privacy.android.app.main.AddContactActivity.EXTRA_CONTACTS
import mega.privacy.android.app.main.AddContactActivity.EXTRA_CONTACT_TYPE
import mega.privacy.android.app.main.AddContactActivity.EXTRA_EKR
import mega.privacy.android.app.main.AddContactActivity.EXTRA_ONLY_CREATE_GROUP
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
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.main.listeners.CreateGroupChatWithPublicLink
import mega.privacy.android.app.main.megachat.ChatExplorerFragment
import mega.privacy.android.app.main.megachat.ChatExplorerListItem
import mega.privacy.android.app.main.megachat.ChatUploadService
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment.Companion.newInstance
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollision.Upload.Companion.getUploadCollision
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.transfers.TransfersManagementActivity
import mega.privacy.android.app.usecase.CopyNodeUseCase
import mega.privacy.android.app.usecase.UploadUseCase
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException.ChildDoesNotExistsException
import mega.privacy.android.app.usecase.exception.MegaNodeException.ParentDoesNotExistException
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ColorUtils.changeStatusBarColorForElevation
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.ColorUtils.tintIcon
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CONTACT_TYPE_MEGA
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil.IS_NEW_FOLDER_DIALOG_SHOWN
import mega.privacy.android.app.utils.MegaNodeDialogUtil.NEW_FOLDER_DIALOG_TEXT
import mega.privacy.android.app.utils.MegaNodeDialogUtil.checkNewFolderDialogState
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showNewFolderDialog
import mega.privacy.android.app.utils.MegaNodeUtil.cloudRootHandle
import mega.privacy.android.app.utils.MegaNodeUtil.existsMyChatFilesFolder
import mega.privacy.android.app.utils.MegaNodeUtil.myChatFilesFolder
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.user.UserCredentials
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
import java.io.IOException
import javax.inject.Inject


/**
 * Activity used for several purposes like import content to the cloud, copies or movements.
 *
 * @property filePrepareUseCase        [FilePrepareUseCase]
 * @property getChatChangesUseCase     [GetChatChangesUseCase]
 * @property checkNameCollisionUseCase [CheckNameCollisionUseCase]
 * @property uploadUseCase             [UploadUseCase]
 * @property copyNodeUseCase           [CopyNodeUseCase]
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
class FileExplorerActivity : TransfersManagementActivity(), MegaRequestListenerInterface,
    MegaGlobalListenerInterface, MegaChatRequestListenerInterface, View.OnClickListener,
    ActionNodeCallback, SnackbarShower {

    @Inject
    lateinit var filePrepareUseCase: FilePrepareUseCase

    @Inject
    lateinit var getChatChangesUseCase: GetChatChangesUseCase

    @Inject
    lateinit var checkNameCollisionUseCase: CheckNameCollisionUseCase

    @Inject
    lateinit var uploadUseCase: UploadUseCase

    @Inject
    lateinit var copyNodeUseCase: CopyNodeUseCase

    private val viewModel by viewModels<FileExplorerViewModel>()

    private lateinit var binding: ActivityFileExplorerBinding

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
    private var gSession: String? = null
    private var credentials: UserCredentials? = null
    private var lastEmail: String? = null
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
    private var filePreparedInfos: List<ShareInfo>? = null
    private var mTabsAdapterExplorer: FileExplorerPagerAdapter? = null
    private var nodes: ArrayList<MegaNode>? = null
    private var importFileF = false
    private var importFragmentSelected = -1
    private var action: String? = null
    private var myChatFilesNode: MegaNode? = null
    private val attachNodes: ArrayList<MegaNode> = ArrayList()
    private val uploadInfos: ArrayList<ShareInfo> = ArrayList()
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

    private val transparentColor by lazy {
        ContextCompat.getColor(
            this,
            android.R.color.transparent
        )
    }

    private val elevation by lazy { resources.getDimension(R.dimen.toolbar_elevation) }
    private val toolbarElevationColor by lazy { getColorForElevation(this, elevation) }

    private lateinit var createChatLauncher: ActivityResultLauncher<Intent?>

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            retryConnectionsAndSignalPresence()
            cDriveExplorer = cloudExplorerFragment
            iSharesExplorer = incomingExplorerFragment

            if (importFileF && importFragmentSelected != CLOUD_FRAGMENT) {
                when (importFragmentSelected) {
                    CHAT_FRAGMENT -> {
                        if (ACTION_UPLOAD_TO_CHAT == action) {
                            finishAndRemoveTask()
                        } else {
                            chatExplorer = chatExplorerFragment

                            if (chatExplorer != null) {
                                chatExplorer?.clearSelections()
                                showFabButton(false)
                                chooseFragment(IMPORT_FRAGMENT)
                            }
                        }
                    }
                    IMPORT_FRAGMENT -> {
                        finishAndRemoveTask()
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
                    finishFileExplorer()
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

    private fun onProcessAsyncInfo(info: List<ShareInfo>?) {
        if (info == null || info.isEmpty()) {
            Timber.w("Selected items list is null or empty.")
            finishFileExplorer()
            return
        }

        filePreparedInfos = info

        if (needLogin) {
            val loginIntent = Intent(this@FileExplorerActivity, LoginActivity::class.java)
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    putExtra(EXTRA_SHARE_ACTION, intent.action)
                    putExtra(EXTRA_SHARE_TYPE, intent.type)
                    putExtra(EXTRA_SHARE_INFOS, ArrayList(info))
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

        if (importFileF) {
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
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        createChatLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode != RESULT_OK) {
                    Timber.d("Result is not OK")
                    return@registerForActivityResult
                }

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

        credentials = dbH.credentials

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
                    StringResourcesUtils.getQuantityString(R.plurals.upload_prepare, 1)
                )
            }
            return
        } else {
            Timber.d("User has credentials")
        }

        if (savedInstanceState != null) {
            folderSelected = savedInstanceState.getBoolean("folderSelected", false)
        }

        binding = ActivityFileExplorerBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setSupportActionBar(binding.toolbarExplorer)
        supportActionBar?.hide()

        binding.fabFileExplorer.setOnClickListener(this)
        showFabButton(false)
        binding.explorerTabsPager.offscreenPageLimit = 3

        if (megaApi.rootNode == null) {
            Timber.d("hide action bar")
            if (!isLoggingIn) {
                isLoggingIn = true
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

                gSession = credentials?.session
                ChatUtil.initMegaChatApi(gSession, this)
                megaApi.fastLogin(gSession, this)
            } else {
                Timber.w("Another login is proccessing")
            }
        } else {
            afterLoginAndFetch()
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )

        checkNotificationsPermission(this)
    }

    private fun setupObservers() {
        nameCollisionActivityContract =
            registerForActivityResult(NameCollisionActivityContract()) { result: String? ->
                backToCloud(
                    if (result != null) parentHandle else INVALID_HANDLE,
                    0,
                    result
                )
            }

        viewModel.filesInfo.observe(this) { info: List<ShareInfo>? ->
            onProcessAsyncInfo(info)
        }
        viewModel.textInfo.observe(this) { dismissAlertDialogIfExists(statusDialog) }

        LiveEventBus.get(EVENT_UPDATE_VIEW_MODE, Boolean::class.java)
            .observe(this) { isList: Boolean -> refreshViewNodes(isList) }
    }

    private fun afterLoginAndFetch() {
        handler = Handler(Looper.getMainLooper())
        Timber.d("SHOW action bar")

        supportActionBar?.apply {
            show()
            Timber.d("supportActionBar.setHomeAsUpIndicator")
            setHomeAsUpIndicator(
                tintIcon(
                    this@FileExplorerActivity,
                    R.drawable.ic_arrow_back_white
                )
            )
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

                    moveFromHandles?.let { handles ->
                        if (handles.isNotEmpty()) {
                            parentMoveCopy =
                                megaApi.getParentNode(megaApi.getNodeByHandle(handles[0]))
                        }
                    }

                    title = getString(R.string.title_share_folder_explorer)
                    setView(SHOW_TABS, true)
                }
                ACTION_PICK_COPY_FOLDER -> {
                    Timber.d("ACTION_PICK_COPY_FOLDER")
                    mode = COPY
                    copyFromHandles = intent.getLongArrayExtra("COPY_FROM")

                    copyFromHandles?.let { handles ->
                        parentMoveCopy =
                            megaApi.getParentNode(megaApi.getNodeByHandle(handles[0]))
                    }

                    title = getString(R.string.title_share_folder_explorer)
                    setView(SHOW_TABS, true)
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
                    parentHandleCloud =
                        intent.getLongExtra(EXTRA_PARENT_HANDLE, INVALID_HANDLE)
                    title = StringResourcesUtils.getString(R.string.section_cloud_drive)
                    supportActionBar?.subtitle =
                        StringResourcesUtils.getString(R.string.cloud_drive_select_destination)
                    setView(CLOUD_TAB, false)
                    tabShown = NO_TABS
                }
                else -> {
                    Timber.d("action = UPLOAD")
                    mode = UPLOAD
                    title = getString(R.string.title_upload_explorer)
                    importFileF = true
                    action = intent.action
                    viewModel.ownFilePrepareTask(this, intent)
                    chooseFragment(IMPORT_FRAGMENT)
                    createAndShowProgressDialog(
                        false,
                        StringResourcesUtils.getQuantityString(R.plurals.upload_prepare, 1)
                    )

                    with(binding) {
                        cloudDriveFrameLayout.isVisible = true
                        slidingTabsFileExplorer.isVisible = false
                        explorerTabsPager.isVisible = false
                    }

                    tabShown = NO_TABS
                }
            }

            title?.let { supportActionBar?.title = it }
        } else {
            Timber.e("intent error")
        }
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
                if (mTabsAdapterExplorer != null) explorerTabsPager.currentItem else 0

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
                    1 -> StringResourcesUtils.getString(R.string.tab_incoming_shares)
                    2 -> StringResourcesUtils.getString(R.string.section_chat)
                    0 -> StringResourcesUtils.getString(R.string.section_cloud_drive)
                    else -> StringResourcesUtils.getString(R.string.section_cloud_drive)
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
        (mTabsAdapterExplorer?.createFragment(position) as CheckScrollInterface).checkScroll()


    /**
     * Shows the choosen fragment.
     *
     * @param fragment The choosen fragment.
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

        changeStatusBarColorForElevation(this, elevate)

        with(binding) {
            if (fragmentIndex == CHAT_FRAGMENT) {
                if (Util.isDarkMode(this@FileExplorerActivity)) {
                    if (tabShown == NO_TABS) {
                        if (elevate) {
                            toolbarExplorer.setBackgroundColor(toolbarElevationColor)
                        } else {
                            toolbarExplorer.setBackgroundColor(transparentColor)
                        }
                    } else {
                        if (elevate) {
                            toolbarExplorer.setBackgroundColor(transparentColor)
                            appBarLayoutExplorer.elevation = elevation
                        } else {
                            toolbarExplorer.setBackgroundColor(transparentColor)
                            appBarLayoutExplorer.elevation = 0F
                        }
                    }
                }
            } else {
                appBarLayoutExplorer.elevation = if (elevate) elevation else 0f
            }
        }
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
            searchView?.findViewById<SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)

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

                    invalidateOptionsMenu()
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
                        querySearch = newText
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
                        searchMenuItem?.isVisible = iSharesExplorer?.isFolderEmpty == false
                    }
                }
                CHAT_TAB -> {
                    searchMenuItem?.isVisible = true
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
                            newChatMenuItem?.isVisible = false
                            searchMenuItem?.isVisible = true
                        }
                    }
                }
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun setRootTitle() {
        Timber.d("setRootTitle")
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
            mode == MOVE || mode == COPY || mode == SELECT_CAMERA_FOLDER || mode == IMPORT -> {
                getString(R.string.title_share_folder_explorer)
            }
            mode == UPLOAD && !importFileF -> {
                getString(R.string.title_file_explorer_send_link)
            }
            mode == SAVE -> {
                StringResourcesUtils.getString(R.string.section_cloud_drive)
            }
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
        supportActionBar?.subtitle = if (mode == SAVE) {
            StringResourcesUtils.getString(R.string.cloud_drive_select_destination)
        } else {
            null
        }

        if (tabShown == NO_TABS || mTabsAdapterExplorer == null) {
            if (importFileF) {
                if (importFragmentSelected != -1) {
                    when (importFragmentSelected) {
                        CLOUD_FRAGMENT -> {
                            if (cDriveExplorer != null) {
                                if (cDriveExplorer?.parentHandle == INVALID_HANDLE
                                    || cDriveExplorer?.parentHandle == megaApi.rootNode.handle
                                ) {
                                    setRootTitle()
                                    supportActionBar?.setSubtitle(R.string.general_select_to_download)
                                } else {
                                    supportActionBar?.setTitle(
                                        megaApi.getNodeByHandle(
                                            cDriveExplorer?.parentHandle ?: return
                                        ).name
                                    )
                                }
                            }
                        }
                        CHAT_FRAGMENT, IMPORT_FRAGMENT -> setRootTitle()
                    }
                }
            } else {
                if (cDriveExplorer != null) {
                    if (cDriveExplorer?.parentHandle == -1L || cDriveExplorer?.parentHandle == megaApi.rootNode.handle) {
                        setRootTitle()
                    } else {
                        supportActionBar?.setTitle(
                            megaApi.getNodeByHandle(
                                cDriveExplorer?.parentHandle
                                    ?: return
                            ).name
                        )
                    }
                }

                showFabButton(false)
            }
        } else {
            val position = binding.explorerTabsPager.currentItem
            val f = mTabsAdapterExplorer?.createFragment(position)

            if (position == 0) {
                if (f is ChatExplorerFragment) {
                    if (tabShown != NO_TABS) {
                        tabShown = CHAT_TAB
                    }

                    supportActionBar?.setTitle(getString(R.string.title_file_explorer_send_link))
                } else if (f is CloudDriveExplorerFragment) {
                    if (tabShown != NO_TABS) {
                        tabShown = CLOUD_TAB
                    }

                    if (f.parentHandle == -1L || f.parentHandle == megaApi.rootNode.handle) {
                        setRootTitle()
                    } else {
                        supportActionBar?.setTitle(megaApi.getNodeByHandle(f.parentHandle).name)
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
                        supportActionBar?.setTitle(megaApi.getNodeByHandle(f.parentHandle).name)
                    }
                } else if (f is CloudDriveExplorerFragment) {
                    if (tabShown != NO_TABS) {
                        tabShown = CLOUD_TAB
                    }

                    if (f.parentHandle == -1L || f.parentHandle == megaApi.rootNode.handle) {
                        setRootTitle()
                    } else {
                        supportActionBar?.setTitle(megaApi.getNodeByHandle(f.parentHandle).name)
                    }
                }
                showFabButton(false)
            } else if (position == 2) {
                if (f is ChatExplorerFragment) {
                    if (tabShown != NO_TABS) {
                        tabShown = CHAT_TAB
                    }

                    supportActionBar?.setTitle(getString(R.string.title_chat_explorer))
                } else if (f is IncomingSharesExplorerFragment) {
                    if (tabShown != NO_TABS) {
                        tabShown = INCOMING_TAB
                    }

                    if (deepBrowserTree == 0) {
                        setRootTitle()
                    } else {
                        supportActionBar?.setTitle(megaApi.getNodeByHandle(f.parentHandle).name)
                    }

                    showFabButton(false)
                }
            }
        }

        invalidateOptionsMenu()
    }

    /**
     * Gets a fragment tag.
     *
     * @param viewPagerId      The pager it.
     * @param fragmentPosition The fragment position.
     * @return The fragment tag.
     */
    fun getFragmentTag(viewPagerId: Int, fragmentPosition: Int): String =
        "android:switcher:$viewPagerId:$fragmentPosition"

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
            chooseFragment(IMPORT_FRAGMENT)
        } else {
            finishAndRemoveTask()
        }
    }

    private val isCloudVisible: Boolean
        get() {
            val isImportingToCloud =
                importFileF && importFragmentSelected == CLOUD_FRAGMENT && binding.explorerTabsPager.currentItem == CLOUD_TAB
            return (cDriveExplorer != null && cDriveExplorer?.isVisible == true
                    && (tabShown == CLOUD_TAB && !importFileF || isImportingToCloud))
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
            filePreparedInfos = null
            openManagerAndFinish()
            return
        }

        Timber.d("Launch chat upload with files %s", filePreparedInfos?.size)
        val notEmptyAttachedNodes = attachNodes.isNotEmpty()
        val notEmptyUploadInfo = uploadInfos.isNotEmpty()
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

        checkNotificationsPermission(this)
        val intent = Intent(this, ChatUploadService::class.java)

        if (notEmptyAttachedNodes) {
            // There are exist files and files for upload
            val attachNodeHandles = LongArray(attachNodes.size)

            for (i in attachNodes.indices) {
                attachNodeHandles[i] = attachNodes[i].handle
            }

            intent.putExtra(ChatUploadService.EXTRA_ATTACH_FILES, attachNodeHandles)
        }
        val attachIdChats = LongArray(chatListItems.size)

        for (i in chatListItems.indices) {
            attachIdChats[i] = chatListItems[i].chatId
        }

        intent.putExtra(ChatUploadService.EXTRA_ATTACH_CHAT_IDS, attachIdChats)

        val infoToShare = if (notEmptyUploadInfo) uploadInfos else filePreparedInfos
        val idPendMsgs = LongArray(uploadInfos.size * chatListItems.size)
        val filesToUploadFingerPrint = HashMap<String, String>()
        var pos = 0

        for (info in infoToShare ?: return) {
            val fingerprint = megaApi.getFingerprint(info.fileAbsolutePath)

            if (fingerprint == null) {
                Timber.w("Error, fingerprint == NULL is not possible to access file for some reason")
                continue
            }

            filesToUploadFingerPrint[fingerprint] = info.fileAbsolutePath

            for (item in chatListItems) {
                val pendingMsg = ChatUtil.createAttachmentPendingMessage(
                    item.chatId,
                    info.fileAbsolutePath, info.getTitle(), true
                )

                idPendMsgs[pos] = pendingMsg.id
                pos++
            }
        }

        intent.putExtra(ChatUploadService.EXTRA_NAME_EDITED, viewModel.fileNames.value)
        intent.putExtra(ChatUploadService.EXTRA_UPLOAD_FILES_FINGERPRINTS, filesToUploadFingerPrint)
        intent.putExtra(ChatUploadService.EXTRA_PEND_MSG_IDS, idPendMsgs)
        intent.putExtra(ChatUploadService.EXTRA_COMES_FROM_FILE_EXPLORER, true)
        intent.putExtra(ChatUploadService.EXTRA_PARENT_NODE, myChatFilesNode?.serialize())
        startService(intent)
        openManagerAndFinish()
    }

    private fun openManagerAndFinish() {
        val intent = Intent(this, ManagerActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    private fun finishFileExplorer() {
        dismissAlertDialogIfExists(statusDialog)
        filePreparedInfos = null
        Timber.d("finish!!!")
        finishAndRemoveTask()
    }

    /**
     * Checks if files exists in MEGA.
     */
    fun checkIfFilesExistsInMEGA() {
        for (info in filePreparedInfos ?: return) {
            val fingerprint = megaApi.getFingerprint(info.fileAbsolutePath)
            val node = megaApi.getNodeByFingerprint(fingerprint)

            if (node != null) {
                if (node.parentHandle == myChatFilesNode?.handle) {
//					File is in My Chat Files --> Add to attach
                    attachNodes.add(node)
                    filesChecked++
                } else {
//					File is in Cloud --> Copy in My Chat Files
                    copyNodeUseCase.copy(node, myChatFilesNode, null)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            filesChecked++
                            attachNodes.add(node)

                            if (filesChecked == (filePreparedInfos ?: return@subscribe).size) {
                                startChatUploadService()
                            }
                        }) { throwable: Throwable? ->
                            filesChecked++
                            Timber.w("Error copying node into My Chat Files")
                            if (filesChecked == (filePreparedInfos ?: return@subscribe).size) {
                                startChatUploadService()
                            }

                            manageCopyMoveException(throwable)
                        }
                }
            } else {
                uploadInfos.add(info)
                filesChecked++
            }
        }

        if (filesChecked == (filePreparedInfos ?: return).size) {
            startChatUploadService()
        }
    }

    /**
     * Handle processed upload intent.
     *
     * @param infos List<ShareInfo> containing all the upload info.
     */
    private fun onIntentProcessed(infos: List<ShareInfo>?) {
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
        val infos = filePreparedInfos

        if (intent != null && intent.action !== ACTION_PROCESSED) {
            intent.action = ACTION_PROCESSED
        }

        Timber.d("intent processed!")

        if (folderSelected) {
            if (infos == null) {
                dismissAlertDialogIfExists(statusDialog)
                showSnackbar(StringResourcesUtils.getString(R.string.upload_can_not_open))
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
                parentHandle = parentNode.handle
            }

            checkNameCollisionUseCase.checkShareInfoList(infos, parentNode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { (collisions, withoutCollisions): Pair<ArrayList<NameCollision>, List<ShareInfo>>, throwable: Throwable? ->
                    dismissAlertDialogIfExists(statusDialog)

                    if (throwable != null) {
                        showSnackbar(StringResourcesUtils.getString(R.string.error_temporary_unavaible))
                    } else {
                        if (collisions.isNotEmpty()) {
                            (nameCollisionActivityContract ?: return@subscribe).launch(collisions)
                        }

                        if (withoutCollisions.isNotEmpty()) {
                            checkNotificationsPermission(this)

                            val text =
                                StringResourcesUtils.getQuantityString(
                                    R.plurals.upload_began,
                                    withoutCollisions.size,
                                    withoutCollisions.size
                                )

                            uploadUseCase.uploadInfos(
                                this,
                                infos,
                                viewModel.fileNames.value,
                                (parentNode ?: return@subscribe).handle
                            )
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    showSnackbar(text)
                                    backToCloud(parentNode.handle, infos.size, null)
                                    filePreparedInfos = null
                                    Timber.d("finish!!!")
                                    finishAndRemoveTask()
                                }) { t: Throwable? -> Timber.e(t) }
                        }
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
                intent.putExtra("MOVE_TO", parentNode.handle)
                intent.putExtra("MOVE_HANDLES", moveFromHandles)
                setResult(RESULT_OK, intent)
                Timber.d("finish!")
                finishAndRemoveTask()
            }
            COPY -> {
                val parentNode = megaApi.getNodeByHandle(handle) ?: megaApi.rootNode
                val intent = Intent()
                intent.putExtra("COPY_TO", parentNode.handle)
                intent.putExtra("COPY_HANDLES", copyFromHandles)
                setResult(RESULT_OK, intent)
                Timber.d("finish!")
                finishAndRemoveTask()
            }
            UPLOAD, SAVE -> {
                Timber.d("mode UPLOAD")
                if (viewModel.isImportingText(intent)) {
                    val parentNode = megaApi.getNodeByHandle(handle) ?: megaApi.rootNode
                    val info = viewModel.textInfoContent
                    val names = viewModel.fileNames.value

                    if (info != null) {
                        val name = if (names != null) names[info.subject] else info.subject
                        createFile(name, info.fileContent, parentNode, info.isUrl)
                    }

                    return
                }

                if (filePreparedInfos == null) {
                    viewModel.ownFilePrepareTask(this, intent)
                    createAndShowProgressDialog(
                        false,
                        StringResourcesUtils.getQuantityString(R.plurals.upload_prepare, 1)
                    )
                } else {
                    onIntentProcessed()
                }
            }
            IMPORT -> {
                val parentNode = megaApi.getNodeByHandle(handle) ?: megaApi.rootNode

                if (tabShown == CLOUD_TAB) {
                    fragmentHandle = megaApi.rootNode.handle
                } else if (tabShown == INCOMING_TAB) {
                    fragmentHandle = -1
                }

                val intent = Intent()
                intent.putExtra("IMPORT_TO", parentNode.handle)
                intent.putExtra("fragmentH", fragmentHandle)

                if (importChatHandles != null) {
                    intent.putExtra("HANDLES_IMPORT_CHAT", importChatHandles)
                }

                setResult(RESULT_OK, intent)
                Timber.d("finish!")
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
                    intent.putExtra(EXTRA_SELECTED_FOLDER, parentNode.handle)
                    intent.putStringArrayListExtra(Constants.SELECTED_CONTACTS, selectedContacts)
                    setResult(RESULT_OK, intent)
                    finishAndRemoveTask()
                }
            }
            SELECT_CAMERA_FOLDER -> {
                val parentNode = megaApi.getNodeByHandle(handle) ?: megaApi.rootNode
                val intent = Intent()
                intent.putExtra("SELECT_MEGA_FOLDER", parentNode.handle)
                setResult(RESULT_OK, intent)
                finishAndRemoveTask()
            }
        }
    }

    /**
     * Goes back to Cloud.
     *
     * @param handle        Parent handle of the folder to open.
     * @param numberUploads Number of uploads.
     * @param message       Message to show.
     */
    private fun backToCloud(handle: Long, numberUploads: Int, message: String?) {
        Timber.d("handle: %s", handle)

        val startIntent = Intent(this, ManagerActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        if (handle != INVALID_HANDLE) {
            startIntent.action = Constants.ACTION_OPEN_FOLDER
            startIntent.putExtra(Constants.INTENT_EXTRA_KEY_PARENT_HANDLE, handle)
        }

        if (numberUploads > 0) {
            startIntent.putExtra(Constants.SHOW_MESSAGE_UPLOAD_STARTED, true)
                .putExtra(Constants.NUMBER_UPLOADS, numberUploads)
        }

        if (message != null) {
            startIntent.putExtra(Constants.EXTRA_MESSAGE, message)
        }

        startActivity(startIntent)
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
            showSnackbar(StringResourcesUtils.getString(R.string.general_text_error))
            return
        }

        parentHandle = parentNode?.handle ?: megaApi.rootNode.handle
        checkNameCollisionUseCase.check(file.name, parentNode)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ handle: Long? ->
                val list = ArrayList<NameCollision>()
                list.add(getUploadCollision(handle ?: return@subscribe, file, parentHandle))
                nameCollisionActivityContract?.launch(list)
            }) { throwable: Throwable? ->
                if (throwable is ParentDoesNotExistException) {
                    showSnackbar(StringResourcesUtils.getString(R.string.general_text_error))
                } else if (throwable is ChildDoesNotExistsException) {
                    checkNotificationsPermission(this)
                    val text = StringResourcesUtils.getQuantityString(R.plurals.upload_began, 1, 1)
                    uploadUseCase.upload(this, file, parentHandle)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                text,
                                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                            )
                            Timber.d("After UPLOAD click - back to Cloud")
                            backToCloud(parentHandle, 1, null)
                            finishAndRemoveTask()
                        }) { t: Throwable? -> Timber.e(t) }
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
    private val currentParentNode: MegaNode
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
                isLoggingIn = false
            } else {
                with(binding) {
                    fileLoginProgressBar.isVisible = true
                    fileLoginFetchingNodesBar.isVisible = false
                    fileLoginLoggingInText.isVisible = true
                    fileLoginFetchNodesText.isVisible = true
                    fileLoginPrepareNodesText.isVisible = false
                }

                gSession = megaApi.dumpSession()
                credentials = UserCredentials(lastEmail, gSession, "", "", "")
                dbH.saveCredentials(credentials ?: return)
                Timber.d("Logged in with session")
                Timber.d("Setting account auth token for folder links.")
                megaApiFolder.accountAuth = megaApi.accountAuth
                megaApi.fetchNodes(this)

                // Get cookies settings after login.
                getInstance().checkEnabledCookies()
            }
        } else if (request.type == MegaRequest.TYPE_FETCH_NODES) {
            if (error.errorCode == MegaError.API_OK) {
                gSession = megaApi.dumpSession()
                val myUser = megaApi.myUser
                var myUserHandle = ""

                if (myUser != null) {
                    lastEmail = megaApi.myUser.email
                    myUserHandle = megaApi.myUser.handle.toString() + ""
                }

                credentials = UserCredentials(lastEmail, gSession, "", "", myUserHandle)
                dbH.saveCredentials(credentials ?: return)
                binding.fileLoggingInLayout.isVisible = false
                isLoggingIn = false
                afterLoginAndFetch()
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

    override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser>) {

    }

    override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert>) {
        Timber.d("onUserAlertsUpdate")
    }

    override fun onEvent(api: MegaApiJava, event: MegaEvent) {}

    override fun onSetsUpdate(api: MegaApiJava?, sets: java.util.ArrayList<MegaSet>?) {
    }

    override fun onSetElementsUpdate(
        api: MegaApiJava?,
        elements: java.util.ArrayList<MegaSetElement>?,
    ) {
    }

    override fun onNodesUpdate(api: MegaApiJava, updatedNodes: ArrayList<MegaNode>) {
        Timber.d("onNodesUpdate")
        cDriveExplorer?.let { cDriveExplorer ->
            if (cloudExplorerFragment != null) {
                if (megaApi.getNodeByHandle(cDriveExplorer.parentHandle) != null) {
                    nodes =
                        megaApi.getChildren(megaApi.getNodeByHandle(parentHandle))
                    nodes?.let {
                        cDriveExplorer.updateNodesByAdapter(it)
                    }
                    cDriveExplorer.recyclerView.invalidate()
                } else {
                    if (megaApi.rootNode != null) {
                        parentHandle = megaApi.rootNode.handle
                        nodes =
                            megaApi.getChildren(megaApi.getNodeByHandle(cDriveExplorer.parentHandle))
                        nodes?.let {
                            cDriveExplorer.updateNodesByAdapter(it)
                        }
                        cDriveExplorer.recyclerView.invalidate()
                    }
                }
            }
        }
    }

    override fun onReloadNeeded(api: MegaApiJava) {

    }

    public override fun onDestroy() {
        megaApi.removeGlobalListener(this)
        val childThumbDir =
            File(ThumbnailUtils.getThumbFolder(this), ImportFilesFragment.THUMB_FOLDER)

        if (FileUtil.isFileAvailable(childThumbDir)) {
            try {
                deleteFile(childThumbDir)
            } catch (e: IOException) {
                Timber.w(e, "IOException deleting childThumbDir.")
            }
        }

        dismissAlertDialogIfExists(newFolderDialog)
        super.onDestroy()
    }

    @Throws(IOException::class)
    private fun deleteFile(file: File) {
        if (file.isDirectory) {
            if (file.list()?.isEmpty() == true) {
                file.delete()
            } else {
                val files = file.list() ?: return

                for (temp in files) {
                    val deleteFile = File(file, temp)
                    deleteFile(deleteFile)
                }

                if (files.isEmpty()) {
                    file.delete()
                }
            }
        } else {
            file.delete()
        }
    }

    override fun onAccountUpdate(api: MegaApiJava) {

    }

    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest>,
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
                    this, this,
                    currentParentNode, null
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
        val users = ArrayList<MegaUser>()
        createAndShowProgressDialog(true, StringResourcesUtils.getString(R.string.preparing_chats))

        for (item in listItems) {
            if (item.chat != null) {
                megaChatApi.getChatRoom(item.chat.chatId)?.let {
                    chats.add(it)
                }
            } else if (item.contact != null && item.contact.megaUser != null) {
                users.add(item.contact.megaUser)
            }
        }
        if (users.isNotEmpty()) {
            val listener = CreateChatListener(
                CreateChatListener.SEND_FILE_EXPLORER_CONTENT,
                chats,
                users,
                this,
                this
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
        if (filePreparedInfos == null) {
            createAndShowProgressDialog(
                false,
                StringResourcesUtils.getQuantityString(R.plurals.upload_prepare, 1)
            )

            filePrepareUseCase.prepareFiles(intent)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { shareInfo: List<ShareInfo>?, throwable: Throwable? ->
                    if (throwable == null) {
                        onIntentProcessed(shareInfo)
                    }
                }
        } else {
            onIntentProcessed(filePreparedInfos)
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
            if (mTabsAdapterExplorer == null) return null
            val c = mTabsAdapterExplorer!!.createFragment(2) as ChatExplorerFragment
            return if (c.isAdded) c else null
        }
    private val incomingExplorerFragment: IncomingSharesExplorerFragment?
        get() {
            if (mTabsAdapterExplorer == null) return null
            val iS = mTabsAdapterExplorer!!.createFragment(1) as IncomingSharesExplorerFragment
            return if (iS.isAdded) iS else null
        }
    private val cloudExplorerFragment: CloudDriveExplorerFragment?
        get() {
            if (tabShown == NO_TABS) {
                return supportFragmentManager.findFragmentByTag("cDriveExplorer") as CloudDriveExplorerFragment?
            }
            if (mTabsAdapterExplorer == null) return null
            val cD = mTabsAdapterExplorer!!.createFragment(0) as CloudDriveExplorerFragment
            return if (cD.isAdded) cD else null
        }

    /**
     * Refresh the order of nodes.
     *
     * @param order Order to apply.
     */
    fun refreshOrderNodes(order: Int) {
        cDriveExplorer = cloudExplorerFragment
        cDriveExplorer?.orderNodes(order)
        iSharesExplorer = incomingExplorerFragment
        iSharesExplorer?.orderNodes(order)
    }

    private fun refreshViewNodes(isList: Boolean) {
        this.isList = isList
        iSharesExplorer = incomingExplorerFragment

        iSharesExplorer?.let {
            supportFragmentManager.beginTransaction().detach(it).commitNowAllowingStateLoss()
            supportFragmentManager.beginTransaction().attach(it).commitNowAllowingStateLoss()
        }
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
     * Gets the paret node to copy.
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
    fun setNameFiles(nameFiles: HashMap<String, String>) {
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
     * View type.
     */
    val itemType: Int
        get() = if (isList) {
            MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
        } else {
            MegaNodeAdapter.ITEM_VIEW_TYPE_GRID
        }

    /**
     * Sets a node as "My chat files" foler.
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
        val chatSubscription = getChatChangesUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .filter { result: GetChatChangesUseCase.Result? -> result is GetChatChangesUseCase.Result.OnChatPresenceLastGreen }
            .map { result: GetChatChangesUseCase.Result -> result as GetChatChangesUseCase.Result.OnChatPresenceLastGreen }
            .subscribe({ (userHandle, lastGreen): GetChatChangesUseCase.Result.OnChatPresenceLastGreen ->
                onChatPresenceLastGreen(userHandle, lastGreen)
            }) { t: Throwable? -> Timber.e(t) }

        composite.add(chatSubscription)
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
         * Intent extra for share infos.
         */
        const val EXTRA_SHARE_INFOS = "share_infos"

        /**
         * Intent extra for share action.
         */
        const val EXTRA_SHARE_ACTION = "share_action"

        /**
         * Intent extra for share type.
         */
        const val EXTRA_SHARE_TYPE = "share_type"

        /**
         * Intent extra for parent handle.
         */
        const val EXTRA_PARENT_HANDLE = "parent_handle"

        /**
         * Intent extra for selected folder.
         */
        const val EXTRA_SELECTED_FOLDER = "selected_folder"

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

        private const val NO_TABS = -1
        private const val CLOUD_TAB = 0
        private const val INCOMING_TAB = 1
        private const val CHAT_TAB = 2
        private const val SHOW_TABS = 3
    }
}