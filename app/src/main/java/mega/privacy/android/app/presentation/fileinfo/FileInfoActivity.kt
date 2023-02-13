package mega.privacy.android.app.presentation.fileinfo

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.DeleteVersionsHistoryActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToCopyActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToMoveActivityContract
import mega.privacy.android.app.activities.contract.SelectUsersToShareActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CREDENTIALS
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FIRST_NAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_LAST_NAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_NICKNAME
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_MANAGE_SHARE
import mega.privacy.android.app.constants.BroadcastConstants.EXTRA_USER_HANDLE
import mega.privacy.android.app.databinding.ActivityFileInfoBinding
import mega.privacy.android.app.databinding.DialogLinkBinding
import mega.privacy.android.app.interfaces.ActionBackupListener
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.listeners.ShareListener
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.FileContactListActivity
import mega.privacy.android.app.main.adapters.MegaFileInfoSharedContactAdapter
import mega.privacy.android.app.main.controllers.ContactController
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.extensions.getQuantityStringOrDefault
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.sync.fileBackups.FileBackupManager
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_EXECUTE
import mega.privacy.android.app.usecase.data.MoveRequestResult
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.CameraUploadUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ChatUtil.StatusIconLocation
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeByHandle
import mega.privacy.android.app.utils.MegaNodeUtil.getFolderIcon
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLocationInfo
import mega.privacy.android.app.utils.MegaNodeUtil.handleLocationClick
import mega.privacy.android.app.utils.MegaNodeUtil.isEmptyFolder
import mega.privacy.android.app.utils.MegaNodeUtil.showConfirmationLeaveIncomingShare
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.PreviewUtils
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
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
import java.util.Locale
import javax.inject.Inject

/**
 * Activity for showing file and folder info.
 *
 * @property passCodeFacade [PasscodeCheck] an injected component to enforce a Passcode security check
 */
@AndroidEntryPoint
class FileInfoActivity : BaseActivity(), ActionNodeCallback, SnackbarShower {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    private val viewModel: FileInfoViewModel by viewModels()

    private val megaRequestListener = object : MegaRequestListenerInterface {
        override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
            Timber.d("onRequestStart: ${request.name}")
        }

        override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {
            Timber.d("onRequestUpdate: ${request?.name}")
        }

        override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, error: MegaError?) {
            this@FileInfoActivity.onRequestFinish(request, error)
        }

        override fun onRequestTemporaryError(
            api: MegaApiJava, request: MegaRequest,
            e: MegaError,
        ) {
            Timber.w("onRequestTemporaryError:  ${request.name}")
        }
    }
    private val megaGlobalListener = object : MegaGlobalListenerInterface {
        override fun onUsersUpdate(api: MegaApiJava?, users: java.util.ArrayList<MegaUser>?) {
            Timber.d("onUsersUpdate")
        }

        override fun onUserAlertsUpdate(
            api: MegaApiJava,
            users: java.util.ArrayList<MegaUserAlert>,
        ) {
            Timber.d("onUserAlertsUpdate")
        }

        override fun onNodesUpdate(api: MegaApiJava, nodes: java.util.ArrayList<MegaNode>?) {
            this@FileInfoActivity.onNodesUpdate(nodes)
        }

        override fun onReloadNeeded(api: MegaApiJava) {
            Timber.d("onReloadNeeded")
        }

        override fun onAccountUpdate(api: MegaApiJava) {
            Timber.d("onAccountUpdate")
        }

        override fun onContactRequestsUpdate(
            api: MegaApiJava,
            requests: java.util.ArrayList<MegaContactRequest>,
        ) {
            Timber.d("onContactRequestsUpdate")
        }

        override fun onEvent(api: MegaApiJava, event: MegaEvent) {
            Timber.d("onEvent")
        }

        override fun onSetsUpdate(api: MegaApiJava, sets: java.util.ArrayList<MegaSet>) {
            Timber.d("onSetsUpdate")
        }

        override fun onSetElementsUpdate(
            api: MegaApiJava,
            elements: java.util.ArrayList<MegaSetElement>,
        ) {
            Timber.d("onSetElementsUpdate")
        }

    }
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            retryConnectionsAndSignalPresence()
            if (isRemoveOffline) {
                val intent = Intent()
                intent.putExtra(NODE_HANDLE, handle)
                setResult(RESULT_OK, intent)
            }
            finish()
        }

    }

    private lateinit var binding: ActivityFileInfoBinding
    private val bindingContent get() = binding.contentFileInfoActivity
    private var firstIncomingLevel = true
    private val nodeAttacher = MegaAttacher(this)
    private val nodeSaver = NodeSaver(
        this, this, this,
        showSaveToDeviceConfirmDialog(this)
    )
    private var fileBackupManager: FileBackupManager? = null
    private val nodeController: NodeController by lazy { NodeController(this) }
    private var nodeVersions: ArrayList<MegaNode>? = null
    private val menuHelper by lazy {
        FileInfoToolbarHelper(
            context = this,
            appBar = binding.appBar,
            toolbar = binding.toolbar,
            collapsingToolbarLayout = binding.fileInfoCollapseToolbar,
            supportActionBar = supportActionBar,
            fileInfoImageLayout = binding.fileInfoImageLayout,
            fileInfoToolbarImage = binding.fileInfoToolbarImage,
            fileInfoIconLayout = binding.fileInfoIconLayout,
        )
    }

    private var isShareContactExpanded = false
    private var owner = true
    private var typeExport = -1
    private var sl: ArrayList<MegaShare>? = null
    private var mOffDelete: MegaOffline? = null
    private var availableOfflineBoolean = false
    private var cC: ContactController? = null
    private var progressDialog: Pair<AlertDialog, FileInfoJobInProgressState?>? = null
    private var publicLink = false
    private val density by lazy { outMetrics.density }
    private var shareIt = true
    private var from = 0
    private var permissionsDialog: AlertDialog? = null
    private var contactMail: String? = null
    private var isRemoveOffline = false
    private var handle: Long = 0
    private var adapterType = 0
    private var selectedShare: MegaShare? = null
    private var listContacts: List<MegaShare> = emptyList()
    private var fullListContacts: List<MegaShare> = emptyList()
    private val adapter: MegaFileInfoSharedContactAdapter by lazy {
        MegaFileInfoSharedContactAdapter(
            this,
            viewModel.node,
            listContacts,
            bindingContent.fileInfoContactListView
        ).apply {
            setShareList(listContacts)
            positionClicked = -1
            isMultipleSelect = false
        }
    }
    private var actionMode: ActionMode? = null
    private var versionsToRemove = 0
    private var versionsRemoved = 0
    private var errorVersionRemove = 0
    private var bottomSheetDialogFragment: FileContactsListBottomSheetDialogFragment? = null
    private val manageShareReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            intent?.let {
                if (adapter.isMultipleSelect) {
                    adapter.clearSelections()
                    hideMultipleSelect()
                }
                adapter.setShareList(listContacts)
                progressDialog?.first?.dismiss()
                permissionsDialog?.dismiss()
            }
        }
    }
    private val contactUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_NICKNAME || intent.action == ACTION_UPDATE_FIRST_NAME || intent.action == ACTION_UPDATE_LAST_NAME || intent.action == ACTION_UPDATE_CREDENTIALS) {
                updateAdapter(intent.getLongExtra(EXTRA_USER_HANDLE, MegaApiJava.INVALID_HANDLE))
            }
        }
    }
    private lateinit var selectContactForShareFolderLauncher: ActivityResultLauncher<MegaNode>
    private lateinit var versionHistoryLauncher: ActivityResultLauncher<Long>
    private lateinit var copyLauncher: ActivityResultLauncher<LongArray>
    private lateinit var moveLauncher: ActivityResultLauncher<LongArray>

    /**
     * activate action mode from adapter
     */
    fun activateActionMode() {
        Timber.d("activateActionMode")
        if (!adapter.isMultipleSelect) {
            adapter.isMultipleSelect = true
            actionMode = startSupportActionMode(ActionBarCallBack())
        }
    }

    /**
     * show snack bar
     */
    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, bindingContent.fileInfoContactListContainer, content, chatId)
    }

    override fun finishRenameActionWithSuccess(newName: String) {
        viewModel.updateNode(megaApi.getNodeByHandle((viewModel.node).handle) ?: return)
        menuHelper.setNodeName(viewModel.node.name)
    }

    override fun actionConfirmed() {
        //No update needed
    }

    override fun createFolder(folderName: String) {
        //No action needed
    }

    private inner class ActionBarCallBack : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            Timber.d("onActionItemClicked")
            val shares = adapter.selectedShares
            when (item.itemId) {
                R.id.action_file_contact_list_permissions -> {
                    //Change permissions
                    val dialogBuilder = MaterialAlertDialogBuilder(
                        this@FileInfoActivity,
                        R.style.ThemeOverlay_Mega_MaterialAlertDialog
                    ).apply {
                        setTitle(getFormattedStringOrDefault(R.string.file_properties_shared_folder_permissions))

                        val items = arrayOf<CharSequence>(
                            getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_only),
                            getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_write),
                            getFormattedStringOrDefault(R.string.file_properties_shared_folder_full_access)
                        )

                        setSingleChoiceItems(items, -1) { _, it ->
                            clearSelections()
                            permissionsDialog?.dismiss()
                            showProgressDialog(
                                getFormattedStringOrDefault(R.string.context_permissions_changing_folder)
                            )
                            cC?.changePermissions(cC?.getEmailShares(shares), it, viewModel.node)
                        }
                    }

                    permissionsDialog = dialogBuilder.show()
                }
                R.id.action_file_contact_list_delete -> {
                    shares?.size?.takeIf { it > 0 }?.let { size ->
                        if (size > 1) {
                            Timber.d("Remove multiple contacts")
                            showConfirmationRemoveMultipleContactFromShare(shares)
                        } else {
                            Timber.d("Remove one contact")
                            showConfirmationRemoveContactFromShare(shares[0].user)
                        }
                    }
                }
                R.id.cab_menu_select_all -> {
                    selectAll()
                }
                R.id.cab_menu_unselect_all -> {
                    clearSelections()
                }
            }
            return false
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            Timber.d("onCreateActionMode")
            mode.menuInflater?.inflate(R.menu.file_contact_shared_browser_action, menu)
            return true
        }

        override fun onDestroyActionMode(arg0: ActionMode) {
            Timber.d("onDestroyActionMode")
            adapter.clearSelections()
            adapter.isMultipleSelect = false
            invalidateOptionsMenu()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Timber.d("onPrepareActionMode")
            val selected: List<MegaShare> = adapter.selectedShares
            var deleteShare = false
            var permissions = false
            if (selected.isNotEmpty()) {
                permissions = true
                deleteShare = true
                val unselect = menu.findItem(R.id.cab_menu_unselect_all)
                menu.findItem(R.id.cab_menu_select_all).isVisible =
                    selected.size != adapter.itemCount
                unselect.title = getFormattedStringOrDefault(R.string.action_unselect_all)
                unselect.isVisible = true
            } else {
                menu.findItem(R.id.cab_menu_select_all).isVisible = true
                menu.findItem(R.id.cab_menu_unselect_all).isVisible = false
            }
            val changePermissionsMenuItem = menu.findItem(R.id.action_file_contact_list_permissions)
            if (viewModel.isNodeInInbox()) {
                // If the node came from Backups, hide the Change Permissions option from the Action Bar
                changePermissionsMenuItem.isVisible = false
                changePermissionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            } else {
                // Otherwise, change Change Permissions visibility depending on whether there are
                // selected contacts or none
                changePermissionsMenuItem.isVisible = permissions
                if (permissions) {
                    changePermissionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                } else {
                    changePermissionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                }
            }
            menu.findItem(R.id.action_file_contact_list_delete).isVisible =
                deleteShare
            if (deleteShare) {
                menu.findItem(R.id.action_file_contact_list_delete)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            } else {
                menu.findItem(R.id.action_file_contact_list_delete)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            }
            return false
        }
    }

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        configureActivityResultLaunchers()
        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            finish()
            return
        }
        viewModel.updateNode(getNodeFromExtras() ?: run {
            finish()
            return
        })
        nodeVersions = megaApi.getVersions(viewModel.node)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        initFileBackupManager()
        cC = ContactController(this)
        adapterType = intent.getIntExtra("adapterType", Constants.FILE_BROWSER_ADAPTER)

        megaApi.addGlobalListener(megaGlobalListener)
        savedInstanceState?.apply {
            val handle = getLong(KEY_SELECTED_SHARE_HANDLE, MegaApiJava.INVALID_HANDLE)
            if (handle == MegaApiJava.INVALID_HANDLE) {
                return
            }
            val list = megaApi.getOutShares(viewModel.node)
            for (share in list) {
                if (handle == share.nodeHandle) {
                    selectedShare = share
                    break
                }
            }
            nodeAttacher.restoreState(this)
            nodeSaver.restoreState(this)
        }

        //register receivers
        registerReceiver(manageShareReceiver, IntentFilter(BROADCAST_ACTION_INTENT_MANAGE_SHARE))
        val contactUpdateFilter = IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE)
        contactUpdateFilter.addAction(ACTION_UPDATE_NICKNAME)
        contactUpdateFilter.addAction(ACTION_UPDATE_FIRST_NAME)
        contactUpdateFilter.addAction(ACTION_UPDATE_LAST_NAME)
        contactUpdateFilter.addAction(ACTION_UPDATE_CREDENTIALS)
        registerReceiver(contactUpdateReceiver, contactUpdateFilter)

        //view
        setupView()
        collectUIState()
    }

    private fun collectUIState() {
        this.collectFlow(viewModel.uiState) { viewState ->
            updateView(viewState)
            updateOptionsMenu(viewState)
            updateProgress(viewState.jobInProgressState)
            viewState.oneOffViewEvent?.let {
                consumeEvent(viewState.oneOffViewEvent)
            }
        }
    }

    private fun updateView(viewState: FileInfoViewState) {
        with(bindingContent) {
            // If the Node belongs to Backups or has no versions, then hide
            // the Versions layout
            if (viewState.showHistoryVersions) {
                filePropertiesVersionsLayout.isVisible = true
                val text = getQuantityStringOrDefault(
                    R.plurals.number_of_versions,
                    viewState.historyVersions,
                    viewState.historyVersions
                )
                filePropertiesTextNumberVersions.text = text
                separatorVersions.isVisible = true
            } else {
                filePropertiesVersionsLayout.isVisible = false
                separatorVersions.isVisible = false
            }
        }
        with(bindingContent) {
            if (!viewModel.node.isTakenDown && !viewState.isNodeInRubbish) {
                filePropertiesSwitch.isEnabled = true
                filePropertiesSwitch.setOnCheckedChangeListener { _: CompoundButton, _: Boolean ->
                    filePropertiesSwitch()
                }
                filePropertiesAvailableOfflineText.setTextColor(
                    ContextCompat.getColor(this@FileInfoActivity, R.color.grey_087_white_087)
                )
            } else {
                filePropertiesSwitch.isEnabled = false
                filePropertiesAvailableOfflineText.setTextColor(
                    ContextCompat.getColor(this@FileInfoActivity, R.color.grey_700_026_grey_300_026)
                )
            }
        }
        refreshProperties(viewState)
    }

    private fun updateOptionsMenu(state: FileInfoViewState) {
        if (state.jobInProgressState == FileInfoJobInProgressState.InitialLoading) return
        menuHelper.updateOptionsMenu(
            node = viewModel.node,
            isInInbox = state.isNodeInInbox,
            isInRubbish = state.isNodeInRubbish,
            fromIncomingShares = from == Constants.FROM_INCOMING_SHARES,
            firstIncomingLevel = firstIncomingLevel,
            nodeAccess = megaApi.getAccess(viewModel.node),
        )
    }

    private fun consumeEvent(event: FileInfoOneOffViewEvent) {
        when (event) {
            FileInfoOneOffViewEvent.NotConnected -> {
                Util.showErrorAlertDialog(
                    getFormattedStringOrDefault(R.string.error_server_connection_problem),
                    false,
                    this
                )
            }
            is FileInfoOneOffViewEvent.GeneralError -> showSnackBar(R.string.general_error)
            is FileInfoOneOffViewEvent.CollisionDetected -> {
                val list = ArrayList<NameCollision>()
                list.add(event.collision)
                nameCollisionActivityContract?.launch(list)
            }
            is FileInfoOneOffViewEvent.Finished -> {
                if (event.exception == null) {
                    showSnackBar(event.successMessage)
                    sendBroadcast(Intent(Constants.BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN))
                    if (event !is FileInfoOneOffViewEvent.Finished.Copying) {

                        // finish after moving the file because the view will be fully updated automatically
                        finish()
                    }
                } else {
                    Timber.e(event.exception)
                    if (!manageCopyMoveException(event.exception)) {
                        showSnackBar(event.failMessage(this))
                    }
                }
            }
        }
        viewModel.consumeOneOffEvent(event)
    }

    private fun showSnackBar(@StringRes resString: Int) =
        showSnackBar(getFormattedStringOrDefault(resString))

    private fun showSnackBar(message: String) {
        showSnackbar(
            Constants.SNACKBAR_TYPE,
            message,
            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
        )
    }

    private fun updateProgress(progressState: FileInfoJobInProgressState?) {
        if (progressState == null) {
            progressDialog?.first?.dismiss()
            progressDialog = null
        } else if (progressDialog?.second != progressState) {
            progressState.progressMessage?.let {
                showProgressDialog(getFormattedStringOrDefault(it), progressState)
            }
        }
    }

    private fun showProgressDialog(message: String, progress: FileInfoJobInProgressState? = null) {
        progressDialog?.first?.dismiss()
        progressDialog = Pair(
            createProgressDialog(
                this@FileInfoActivity,
                message
            ).also { it.show() },
            progress
        )
    }

    private fun getNodeFromExtras(): MegaNode? {
        val extras = intent.extras
        if (extras != null) {
            from = extras.getInt("from")
            if (from == Constants.FROM_INCOMING_SHARES) {
                firstIncomingLevel = extras.getBoolean(Constants.INTENT_EXTRA_KEY_FIRST_LEVEL)
            }
            val handleNode = extras.getLong("handle", -1)
            Timber.d("Handle of the selected node: %s", handleNode)
            return megaApi.getNodeByHandle(handleNode) ?: run {
                Timber.w("Node is NULL")
                null
            }

        } else {
            Timber.w("Extras is NULL")
            return null
        }
    }

    private fun setupView() {
        binding = ActivityFileInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            setSupportActionBar(toolbar)
            val rect = Rect()
            window.decorView.getWindowVisibleDisplayFrame(rect)
            menuHelper.setupToolbar(
                isCollapsable = viewModel.node.hasPreview() || viewModel.node.hasThumbnail(),
                nestedView = bindingContent.nestedLayout,
                visibleTop = rect.top
            )
            filePropertiesPermissionInfo.isVisible = false
        }

        with(bindingContent) {
            //Available Offline Layout
            availableOfflineLayout.isVisible = true

            //Share with Layout
            filePropertiesSharedLayout.setOnClickListener {
                sharedContactClicked()
            }
            filePropertiesSharedInfoButton.setOnClickListener {
                sharedContactClicked()
            }
            //Owner Layout
            val ownerString = "(${getFormattedStringOrDefault(R.string.file_properties_owner)})"
            filePropertiesOwnerLabelOwner.text = ownerString
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                filePropertiesOwnerLabel.setMaxWidthEmojis(Util.dp2px(MAX_WIDTH_FILENAME_LAND))
                filePropertiesOwnerInfo.maxWidth = Util.dp2px(MAX_WIDTH_FILENAME_LAND_2)
            } else {
                filePropertiesOwnerLabel.setMaxWidthEmojis(Util.dp2px(MAX_WIDTH_FILENAME_PORT))
                filePropertiesOwnerInfo.maxWidth = Util.dp2px(MAX_WIDTH_FILENAME_PORT_2)
            }
            filePropertiesOwnerLayout.isVisible = false

            //Folder Versions Layout
            filePropertiesFolderVersionsLayout.isVisible = false
            filePropertiesFolderCurrentVersionsLayout.isVisible = false
            filePropertiesFolderPreviousVersionsLayout.isVisible = false

            //Content Layout
            filePropertiesLinkButton.text = getFormattedStringOrDefault(R.string.context_copy)
            filePropertiesLinkButton.setOnClickListener {
                Timber.d("Copy link button")
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Text", viewModel.node.publicLink)
                clipboard.setPrimaryClip(clip)
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getFormattedStringOrDefault(R.string.file_properties_get_link),
                    -1
                )
            }

            val listView = fileInfoContactListView
            listView.itemAnimator = Util.noChangeRecyclerViewItemAnimator()
            listView.addItemDecoration(SimpleDividerItemDecoration(this@FileInfoActivity))
            val mLayoutManager = LinearLayoutManager(this@FileInfoActivity)
            listView.layoutManager = mLayoutManager

            //get shared contact list and max number can be displayed in the list is five
            setContactList()
            moreButton.setOnClickListener {
                val i = Intent(this@FileInfoActivity, FileContactListActivity::class.java)
                i.putExtra(Constants.NAME, viewModel.node.handle)
                startActivity(i)
            }
            setMoreButtonText()

            //setup adapter
            listView.adapter = adapter
            //Location Layout
            getNodeLocationInfo(
                adapterType, from == Constants.FROM_INCOMING_SHARES,
                viewModel.node.handle
            )?.let { locationInfo ->
                with(filePropertiesInfoDataLocation) {
                    text = locationInfo.location
                    setOnClickListener {
                        handleLocationClick(this@FileInfoActivity, adapterType, locationInfo)
                    }
                }
            } ?: run {
                filePropertiesLocationLayout.isVisible = false
            }

            if (viewModel.node.isFolder) {
                filePropertiesCreatedLayout.isVisible = false
                if (isEmptyFolder(viewModel.node)) {
                    availableOfflineLayout.isVisible = false
                    availableOfflineSeparator.isVisible = false
                }
            } else {
                filePropertiesCreatedLayout.isVisible = true
            }
            menuHelper.setNodeName(viewModel.node.name)
            // If the Node belongs to Backups or has no versions, then hide
            // the Versions layout
            filePropertiesTextNumberVersions.setOnClickListener {
                versionHistoryLauncher.launch(viewModel.node.handle)
            }
        }
        setIconResource()
    }

    /**
     * Initializes the FileBackupManager
     */
    private fun initFileBackupManager() {
        fileBackupManager = FileBackupManager(
            this,
            object : ActionBackupListener {
                override fun actionBackupResult(
                    actionType: Int,
                    operationType: Int,
                    result: MoveRequestResult?,
                    handle: Long,
                ) {
                    if (actionType == ACTION_BACKUP_SHARE_FOLDER && operationType == OPERATION_EXECUTE) {
                        shareFolder()
                    }
                }
            })
    }


    private fun setOwnerState(userHandle: Long) =
        ChatUtil.setContactStatus(
            megaChatApi.getUserOnlineStatus(userHandle),
            bindingContent.filePropertiesOwnerStateIcon,
            StatusIconLocation.STANDARD
        )

    /**
     * creates the options menu for this activity
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.file_info_action, menu)
        menuHelper.setupOptionsMenu(menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * performs the action corresponding to the menu item selected
     */
    @SuppressLint("NonConstantResourceId")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
            R.id.cab_menu_file_info_download -> {
                checkNotificationsPermission(this)
                nodeSaver.saveNode(
                    viewModel.node,
                    highPriority = false,
                    isFolderLink = false,
                    fromMediaViewer = false,
                    needSerialize = false
                )
            }
            R.id.cab_menu_file_info_share_folder -> {
                val nodeType = checkBackupNodeTypeByHandle(megaApi, viewModel.node)
                if (nodeType != BACKUP_NONE) {
                    // Display a warning dialog when sharing a Backup folder and limit folder
                    // access to read-only
                    fileBackupManager?.defaultActionBackupNodeCallback?.let {
                        fileBackupManager?.shareBackupFolder(
                            nodeController,
                            viewModel.node,
                            nodeType,
                            ACTION_BACKUP_SHARE_FOLDER,
                            it
                        )
                    }
                } else {
                    shareFolder()
                }
            }
            R.id.cab_menu_file_info_get_link, R.id.cab_menu_file_info_edit_link -> {
                if (showTakenDownNodeActionNotAvailableDialog(viewModel.node, this)) {
                    return false
                }
                LinksUtil.showGetLinkActivity(this, viewModel.node.handle)
            }
            R.id.cab_menu_file_info_remove_link -> {
                if (showTakenDownNodeActionNotAvailableDialog(viewModel.node, this)) {
                    return false
                }
                shareIt = false
                val dialogLayout = DialogLinkBinding.inflate(layoutInflater).apply {
                    (dialogLinkTextRemove.layoutParams as RelativeLayout.LayoutParams).setMargins(
                        Util.scaleWidthPx(
                            25,
                            outMetrics
                        ), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(10, outMetrics), 0
                    )
                    dialogLinkLinkUrl.isVisible = false
                    dialogLinkLinkKey.isVisible = false
                    dialogLinkSymbol.isVisible = false
                    dialogLinkTextRemove.isVisible = true
                    dialogLinkTextRemove.text =
                        getFormattedStringOrDefault(R.string.context_remove_link_warning_text)

                    val isLandscape =
                        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    val size = if (isLandscape) 10 else 15
                    val scaleW = Util.getScaleW(outMetrics, density)
                    dialogLinkTextRemove.setTextSize(TypedValue.COMPLEX_UNIT_SP, size * scaleW)
                }

                MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                    .setView(dialogLayout.root)
                    .setPositiveButton(getFormattedStringOrDefault(R.string.context_remove)) { _: DialogInterface?, _: Int ->
                        typeExport = TYPE_EXPORT_REMOVE
                        megaApi.disableExport(viewModel.node, megaRequestListener)
                    }.setNegativeButton(getFormattedStringOrDefault(R.string.general_cancel), null)
                    .show()
            }
            R.id.cab_menu_file_info_copy -> copyLauncher.launch(longArrayOf(viewModel.node.handle))
            R.id.cab_menu_file_info_move -> moveLauncher.launch(longArrayOf(viewModel.node.handle))
            R.id.cab_menu_file_info_rename -> {
                showRenameNodeDialog(this, viewModel.node, this, this)
            }
            R.id.cab_menu_file_info_leave -> showConfirmationLeaveIncomingShare(
                this,
                this,
                viewModel.node
            )
            R.id.cab_menu_file_info_rubbish, R.id.cab_menu_file_info_delete -> {
                moveToTrash()
            }
            R.id.cab_menu_file_info_send_to_chat -> {
                Timber.d("Send chat option")
                nodeAttacher.attachNode(viewModel.node)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun configureActivityResultLaunchers() {
        configureSelectContactForShareFolderLauncher()
        configureVersionHistoryLauncher()
        configureCopyLauncher()
        configureMoveLauncher()
    }

    private fun configureSelectContactForShareFolderLauncher() {
        selectContactForShareFolderLauncher =
            registerForActivityResult(SelectUsersToShareActivityContract()) { result ->
                if (!viewModel.checkAndHandleIsDeviceConnected()) {
                    return@registerForActivityResult
                }
                result?.let {
                    val contactsData = ArrayList<String>().apply { addAll(result) }
                    if (viewModel.node.isFolder) {
                        if (fileBackupManager?.shareFolder(
                                nodeController,
                                longArrayOf(viewModel.node.handle),
                                contactsData,
                                MegaShare.ACCESS_READ
                            ) != true
                        ) {
                            val items = arrayOf<CharSequence>(
                                getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_only),
                                getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_write),
                                getFormattedStringOrDefault(R.string.file_properties_shared_folder_full_access)
                            )
                            permissionsDialog =
                                MaterialAlertDialogBuilder(
                                    this,
                                    R.style.ThemeOverlay_Mega_MaterialAlertDialog
                                )
                                    .setTitle(getFormattedStringOrDefault(R.string.file_properties_shared_folder_permissions))
                                    .setSingleChoiceItems(items, -1) { _, item ->
                                        showProgressDialog(
                                            getFormattedStringOrDefault(R.string.context_sharing_folder)
                                        )
                                        permissionsDialog?.dismiss()
                                        nodeController.shareFolder(
                                            viewModel.node,
                                            contactsData,
                                            item
                                        )
                                    }.show()
                        }
                    } else {
                        Timber.w("ERROR, the file is not folder")
                    }
                }
            }
    }

    private fun configureVersionHistoryLauncher() {
        versionHistoryLauncher =
            registerForActivityResult(DeleteVersionsHistoryActivityContract()) { result ->
                if (!viewModel.checkAndHandleIsDeviceConnected()) {
                    return@registerForActivityResult
                }
                if (result == viewModel.node.handle) {
                    val versions = megaApi.getVersions(viewModel.node)
                    versionsToRemove = versions.size - 1
                    for (i in 1 until versions.size) {
                        megaApi.removeVersion(versions[i], megaRequestListener)
                    }
                }
            }
    }

    private fun configureCopyLauncher() {
        copyLauncher =
            registerForActivityResult(SelectFolderToCopyActivityContract()) { result ->
                result?.second?.let { selectedFolderNode ->
                    viewModel.copyNodeCheckingCollisions(parentHandle = NodeId(selectedFolderNode))
                }
            }
    }

    private fun configureMoveLauncher() {
        moveLauncher =
            registerForActivityResult(SelectFolderToMoveActivityContract()) { result ->
                result?.second?.let { selectedFolderNode ->
                    viewModel.moveNodeCheckingCollisions(parentHandle = NodeId(selectedFolderNode))
                }
            }
    }

    /**
     * Starts a new Intent to share the folder to different contacts
     */
    private fun shareFolder() {
        selectContactForShareFolderLauncher.launch(viewModel.node)
    }

    private fun refreshProperties(viewState: FileInfoViewState) {
        Timber.d("refreshProperties")
        if (!viewModel.node.isTakenDown && viewModel.node.isExported) {
            publicLink = true
            bindingContent.dividerLinkLayout.isVisible = true
            bindingContent.filePropertiesLinkLayout.isVisible = true
            bindingContent.filePropertiesCopyLayout.isVisible = true
            bindingContent.filePropertiesLinkText.text = viewModel.node.publicLink
            bindingContent.filePropertiesLinkDate.text = getFormattedStringOrDefault(
                R.string.general_date_label, TimeUtils.formatLongDateTime(
                    viewModel.node.publicLinkCreationTime
                )
            )
        } else {
            publicLink = false
            bindingContent.dividerLinkLayout.isVisible = false
            bindingContent.filePropertiesLinkLayout.isVisible = false
            bindingContent.filePropertiesCopyLayout.isVisible = false
        }

        if (viewModel.node.isFile) {
            Timber.d("Node is FILE")
            bindingContent.filePropertiesSharedLayout.isVisible = false
            bindingContent.dividerSharedLayout.isVisible = false
            bindingContent.filePropertiesInfoMenuSize.text =
                getFormattedStringOrDefault(R.string.file_properties_info_size_file)
            bindingContent.filePropertiesInfoDataSize.text = Util.getSizeString(viewModel.node.size)
            bindingContent.filePropertiesContentLayout.isVisible = false
            if (viewModel.node.creationTime != 0L) {
                try {
                    bindingContent.filePropertiesInfoDataAdded.text =
                        TimeUtils.formatLongDateTime(
                            viewModel.node.creationTime
                        )
                } catch (ex: Exception) {
                    bindingContent.filePropertiesInfoDataAdded.text = ""
                }
                if (viewModel.node.modificationTime != 0L) {
                    try {
                        bindingContent.filePropertiesInfoDataCreated.text =
                            TimeUtils.formatLongDateTime(
                                viewModel.node.modificationTime
                            )
                    } catch (ex: Exception) {
                        bindingContent.filePropertiesInfoDataCreated.text = ""
                    }
                } else {
                    try {
                        bindingContent.filePropertiesInfoDataCreated.text =
                            TimeUtils.formatLongDateTime(
                                viewModel.node.creationTime
                            )
                    } catch (ex: Exception) {
                        bindingContent.filePropertiesInfoDataCreated.text = ""
                    }
                }
            } else {
                bindingContent.filePropertiesInfoDataAdded.text = ""
                bindingContent.filePropertiesInfoDataCreated.text = ""
            }

            val preview = PreviewUtils.getPreviewFromCache(viewModel.node)
                ?: PreviewUtils.getPreviewFromFolder(viewModel.node, this)
            if (preview == null) {
                if (viewModel.node.hasPreview()) {
                    val previewFile =
                        File(
                            PreviewUtils.getPreviewFolder(this),
                            "${viewModel.node.base64Handle}.jpg"
                        )
                    megaApi.getPreview(
                        viewModel.node,
                        previewFile.absolutePath,
                        megaRequestListener
                    )
                }
            }
            (preview ?: ThumbnailUtils.getThumbnailFromCache(viewModel.node)
            ?: ThumbnailUtils.getThumbnailFromFolder(viewModel.node, this))?.let {
                menuHelper.setPreview(it)
            }

            // If the Node belongs to Backups or has no versions, then hide
            // the Versions layout
            if (!viewState.isNodeInInbox && megaApi.hasVersions(viewModel.node)) {
                nodeVersions = megaApi.getVersions(viewModel.node)
            }
        } else if (viewModel.node.isFolder) {
            Timber.d("Node is FOLDER")
            megaApi.getFolderInfo(viewModel.node, megaRequestListener)
            bindingContent.filePropertiesInfoDataContent.isVisible = true
            bindingContent.filePropertiesInfoMenuContent.isVisible = true
            bindingContent.filePropertiesInfoDataContent.text =
                MegaApiUtils.getMegaNodeFolderInfo(viewModel.node)
            val sizeFile = megaApi.getSize(viewModel.node)
            bindingContent.filePropertiesInfoDataSize.text = Util.getSizeString(sizeFile)
            setIconResource()
            if (from == Constants.FROM_INCOMING_SHARES) {
                //Show who is the owner
                bindingContent.contactListThumbnail.setImageBitmap(null)
                val sharesIncoming = megaApi.inSharesList
                for (j in sharesIncoming.indices) {
                    val mS = sharesIncoming[j]
                    if (mS.nodeHandle == viewModel.node.handle) {
                        val user = megaApi.getContact(mS.user)
                        contactMail = user?.email
                        if (user != null) {
                            val name = ContactUtil.getMegaUserNameDB(user) ?: user.email
                            bindingContent.filePropertiesOwnerLabel.text = name
                            bindingContent.filePropertiesOwnerInfo.text = user.email
                            setOwnerState(user.handle)
                            createDefaultAvatar(bindingContent.contactListThumbnail, user, name)
                        } else {
                            bindingContent.filePropertiesOwnerLabel.text = mS.user
                            bindingContent.filePropertiesOwnerInfo.text = mS.user
                            setOwnerState(-1)
                            createDefaultAvatar(bindingContent.contactListThumbnail, null, mS.user)
                        }
                        val avatar = buildAvatarFile(this, "$contactMail.jpg")
                        var bitmap: Bitmap?
                        if (FileUtil.isFileAvailable(avatar)) {
                            avatar?.takeIf { it.length() > 0 }?.let { avatarNoEmpty ->
                                val bOpts = BitmapFactory.Options()
                                bitmap = BitmapFactory.decodeFile(avatarNoEmpty.absolutePath, bOpts)
                                if (bitmap == null) {
                                    avatarNoEmpty.delete()
                                    megaApi.getUserAvatar(
                                        user,
                                        buildAvatarFile(this, "$contactMail.jpg")?.absolutePath,
                                        megaRequestListener
                                    )
                                } else {
                                    bindingContent.contactListThumbnail.setImageBitmap(bitmap)
                                }
                            } ?: run {
                                //avatar.length == 0
                                megaApi.getUserAvatar(
                                    user,
                                    buildAvatarFile(this, "$contactMail.jpg")?.absolutePath,
                                    megaRequestListener
                                )
                            }
                        } else {
                            megaApi.getUserAvatar(
                                user,
                                buildAvatarFile(this, "$contactMail.jpg")?.absolutePath,
                                megaRequestListener
                            )
                        }
                        bindingContent.filePropertiesOwnerLayout.isVisible = true
                    }
                }
            }
            sl = megaApi.getOutShares(viewModel.node)
            sl?.let { sl ->
                if (sl.size == 0) {
                    bindingContent.filePropertiesSharedLayout.isVisible = false
                    bindingContent.dividerSharedLayout.isVisible = false
                    //If I am the owner
                    if (megaApi.checkAccessErrorExtended(
                            viewModel.node,
                            MegaShare.ACCESS_OWNER
                        ).errorCode == MegaError.API_OK
                    ) {
                        binding.filePropertiesPermissionInfo.isVisible = false
                    } else {
                        owner = false
                        //If I am not the owner
                        binding.filePropertiesPermissionInfo.isVisible = true
                        val accessLevel = megaApi.getAccess(viewModel.node)
                        Timber.d("Node: %s", viewModel.node.handle)
                        when (accessLevel) {
                            MegaShare.ACCESS_OWNER, MegaShare.ACCESS_FULL -> {
                                binding.filePropertiesPermissionInfo.text =
                                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_full_access)
                                        .uppercase(Locale.getDefault())
                            }
                            MegaShare.ACCESS_READ -> {
                                binding.filePropertiesPermissionInfo.text =
                                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_only)
                                        .uppercase(Locale.getDefault())
                            }
                            MegaShare.ACCESS_READWRITE -> {
                                binding.filePropertiesPermissionInfo.text =
                                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_write)
                                        .uppercase(Locale.getDefault())
                            }
                        }
                    }
                } else {
                    bindingContent.filePropertiesSharedLayout.isVisible = true
                    bindingContent.dividerSharedLayout.isVisible = true
                    bindingContent.filePropertiesSharedInfoButton.text =
                        getQuantityStringOrDefault(
                            R.plurals.general_selection_num_contacts,
                            sl.size, sl.size
                        )
                }
                if (viewModel.node.creationTime != 0L) {
                    try {
                        bindingContent.filePropertiesInfoDataAdded.text =
                            DateUtils.getRelativeTimeSpanString(
                                viewModel.node.creationTime * 1000
                            )
                    } catch (ex: Exception) {
                        bindingContent.filePropertiesInfoDataAdded.text = ""
                    }
                    if (viewModel.node.modificationTime != 0L) {
                        try {
                            bindingContent.filePropertiesInfoDataCreated.text =
                                DateUtils.getRelativeTimeSpanString(
                                    viewModel.node.modificationTime * 1000
                                )
                        } catch (ex: Exception) {
                            bindingContent.filePropertiesInfoDataCreated.text = ""
                        }
                    } else {
                        try {
                            bindingContent.filePropertiesInfoDataCreated.text =
                                DateUtils.getRelativeTimeSpanString(
                                    viewModel.node.creationTime * 1000
                                )
                        } catch (ex: Exception) {
                            bindingContent.filePropertiesInfoDataCreated.text = ""
                        }
                    }
                } else {
                    bindingContent.filePropertiesInfoDataAdded.text = ""
                    bindingContent.filePropertiesInfoDataCreated.text = ""
                }
            } ?: run {
                bindingContent.filePropertiesSharedLayout.isVisible = false
                bindingContent.dividerSharedLayout.isVisible = false
            }
        }

        //Choose the button bindingContent.filePropertiesSwitch
        if (OfflineUtils.availableOffline(this, viewModel.node)) {
            availableOfflineBoolean = true
            bindingContent.filePropertiesSwitch.isChecked = true
            return
        }
        availableOfflineBoolean = false
        bindingContent.filePropertiesSwitch.isChecked = false
    }

    private fun createDefaultAvatar(
        contactListThumbnail: ImageView,
        user: MegaUser?,
        name: String?,
    ) = contactListThumbnail.setImageBitmap(
        AvatarUtil.getDefaultAvatar(
            AvatarUtil.getColorAvatar(user),
            name,
            Constants.AVATAR_SIZE,
            true
        )
    )

    private fun sharedContactClicked() {
        val sharedContactLayout = bindingContent.sharedContactListContainer
        if (isShareContactExpanded) {
            sl?.let { sl ->
                bindingContent.filePropertiesSharedInfoButton.text =
                    getQuantityStringOrDefault(
                        R.plurals.general_selection_num_contacts,
                        sl.size, sl.size
                    )
            }
            sharedContactLayout.isVisible = false
        } else {
            bindingContent.filePropertiesSharedInfoButton.setText(R.string.general_close)
            sharedContactLayout.isVisible = true
        }
        isShareContactExpanded = !isShareContactExpanded
    }

    private fun filePropertiesSwitch() {
        val isChecked = bindingContent.filePropertiesSwitch.isChecked
        if (viewModel.getStorageState() === StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            bindingContent.filePropertiesSwitch.isChecked = !isChecked
            return
        }
        if (owner) {
            Timber.d("Owner: me")
            if (!isChecked) {
                Timber.d("isChecked")
                isRemoveOffline = true
                handle = viewModel.node.handle
                availableOfflineBoolean = false
                bindingContent.filePropertiesSwitch.isChecked = false
                mOffDelete = dbH.findByHandle(handle)
                OfflineUtils.removeOffline(mOffDelete, dbH, this)
                showSnackbar(getFormattedStringOrDefault(R.string.file_removed_offline))
            } else {
                Timber.d("NOT Checked")
                isRemoveOffline = false
                handle = -1
                availableOfflineBoolean = true
                bindingContent.filePropertiesSwitch.isChecked = true
                val destination =
                    OfflineUtils.getOfflineParentFile(this, from, viewModel.node, megaApi)
                Timber.d("Path destination: %s", destination)
                if (FileUtil.isFileAvailable(destination) && destination.isDirectory) {
                    val offlineFile = File(destination, viewModel.node.name)
                    if (FileUtil.isFileAvailable(offlineFile) && viewModel.node.size == offlineFile.length() && offlineFile.name == viewModel.node.name) {
                        //This means that is already available offline
                        return
                    }
                }
                Timber.d("Handle to save for offline : ${viewModel.node.handle}")
                OfflineUtils.saveOffline(destination, viewModel.node, this)
            }
            invalidateOptionsMenu()
        } else {
            Timber.d("Not owner")
            if (!isChecked) {
                availableOfflineBoolean = false
                bindingContent.filePropertiesSwitch.isChecked = false
                mOffDelete = dbH.findByHandle(viewModel.node.handle)
                OfflineUtils.removeOffline(mOffDelete, dbH, this)
                invalidateOptionsMenu()
            } else {
                availableOfflineBoolean = true
                bindingContent.filePropertiesSwitch.isChecked = true
                invalidateOptionsMenu()
                Timber.d("Checking the node%s", viewModel.node.handle)

                //check the parent
                val result = OfflineUtils.findIncomingParentHandle(viewModel.node, megaApi)
                Timber.d("IncomingParentHandle: %s", result)
                if (result != -1L) {
                    val destination = OfflineUtils.getOfflineParentFile(
                        this,
                        Constants.FROM_INCOMING_SHARES,
                        viewModel.node,
                        megaApi
                    )
                    if (FileUtil.isFileAvailable(destination) && destination.isDirectory) {
                        val offlineFile = File(destination, viewModel.node.name)
                        if (FileUtil.isFileAvailable(offlineFile) && viewModel.node.size == offlineFile.length() && offlineFile.name == viewModel.node.name) { //This means that is already available offline
                            return
                        }
                    }
                    OfflineUtils.saveOffline(destination, viewModel.node, this)
                } else {
                    Timber.w("result=findIncomingParentHandle NOT result!")
                }
            }
        }
    }

    private fun moveToTrash() {
        Timber.d("moveToTrash")
        if (!viewModel.checkAndHandleIsDeviceConnected()) {
            return
        }
        if (isFinishing) {
            return
        }

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog).apply {
            val messageId = if (!viewModel.uiState.value.isNodeInRubbish) {
                when {
                    CameraUploadUtil.getPrimaryFolderHandle() == handle && CameraUploadUtil.isPrimaryEnabled() -> {
                        R.string.confirmation_move_cu_folder_to_rubbish
                    }
                    CameraUploadUtil.getSecondaryFolderHandle() == handle && CameraUploadUtil.isSecondaryEnabled() -> {
                        R.string.confirmation_move_mu_folder_to_rubbish
                    }
                    else -> {
                        R.string.confirmation_move_to_rubbish
                    }
                }
            } else {
                R.string.confirmation_delete_from_mega
            }

            setMessage(messageId)
            setPositiveButton(R.string.general_remove) { _: DialogInterface?, _: Int ->
                viewModel.removeNode()
            }
            setNegativeButton(R.string.general_cancel, null)
            show()
        }
    }

    @SuppressLint("NewApi")
    private fun onRequestFinish(request: MegaRequest, e: MegaError?) {
        if (adapter.isMultipleSelect) {
            adapter.clearSelections()
            hideMultipleSelect()
        }
        Timber.d("onRequestFinish: %d__%s", request.type, request.requestString)
        if (request.type == MegaRequest.TYPE_GET_ATTR_FILE) {
            if (e?.errorCode == MegaError.API_OK) {
                val previewDir = PreviewUtils.getPreviewFolder(this)
                val preview = File(previewDir, viewModel.node.base64Handle + ".jpg")
                if (preview.exists()) {
                    if (preview.length() > 0) {
                        PreviewUtils.getBitmapForCache(preview, this)?.also { bitmap ->
                            PreviewUtils.previewCache.put(viewModel.node.handle, bitmap)
                            menuHelper.setPreview(bitmap)
                        }
                    }
                }
            }
        } else if (request.type == MegaRequest.TYPE_FOLDER_INFO) {

            // If the Folder belongs to Backups, hide all Folder Version layouts
            if (viewModel.isNodeInInbox()) {
                bindingContent.filePropertiesFolderVersionsLayout.isVisible = false
                bindingContent.filePropertiesFolderCurrentVersionsLayout.isVisible = false
                bindingContent.filePropertiesFolderPreviousVersionsLayout.isVisible = false
                return
            }
            if (e?.errorCode == MegaError.API_OK) {
                val info = request.megaFolderInfo
                val numVersions = info.numVersions
                Timber.d("Num versions: %s", numVersions)
                if (numVersions > 0) {
                    bindingContent.filePropertiesFolderVersionsLayout.isVisible = true
                    val text = getQuantityStringOrDefault(
                        R.plurals.number_of_versions_inside_folder,
                        numVersions,
                        numVersions
                    )
                    bindingContent.filePropertiesInfoDataFolderVersions.text = text
                    val currentVersions = info.currentSize
                    Timber.d("Current versions: %s", currentVersions)
                    if (currentVersions > 0) {
                        bindingContent.filePropertiesInfoDataFolderCurrentVersions.text =
                            Util.getSizeString(currentVersions)
                        bindingContent.filePropertiesFolderCurrentVersionsLayout.visibility =
                            View.VISIBLE
                    }
                } else {
                    bindingContent.filePropertiesFolderVersionsLayout.isVisible = false
                    bindingContent.filePropertiesFolderCurrentVersionsLayout.visibility =
                        View.GONE
                }
                val previousVersions = info.versionsSize
                Timber.d("Previous versions: %s", previousVersions)
                if (previousVersions > 0) {
                    bindingContent.filePropertiesInfoDataFolderPreviousVersions.text =
                        Util.getSizeString(previousVersions)
                    bindingContent.filePropertiesFolderPreviousVersionsLayout.visibility =
                        View.VISIBLE
                } else {
                    bindingContent.filePropertiesFolderPreviousVersionsLayout.visibility =
                        View.GONE
                }
            } else {
                bindingContent.filePropertiesFolderPreviousVersionsLayout.isVisible = false
                bindingContent.filePropertiesFolderVersionsLayout.isVisible = false
                bindingContent.filePropertiesFolderCurrentVersionsLayout.isVisible = false
            }
        } else if (request.type == MegaRequest.TYPE_REMOVE) {
            if (versionsToRemove > 0) {
                Timber.d("Remove request finished")
                if (e?.errorCode == MegaError.API_OK) {
                    versionsRemoved++
                } else {
                    errorVersionRemove++
                }
                if (versionsRemoved + errorVersionRemove == versionsToRemove) {
                    if (versionsRemoved == versionsToRemove) {
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getFormattedStringOrDefault(R.string.version_history_deleted),
                            -1
                        )
                    } else {
                        val firstLine = getFormattedStringOrDefault(
                            R.string.version_history_deleted_erroneously
                        )
                        val secondLine = getQuantityStringOrDefault(
                            R.plurals.versions_deleted_succesfully,
                            versionsRemoved,
                            versionsRemoved
                        )
                        val thirdLine = getQuantityStringOrDefault(
                            R.plurals.versions_not_deleted,
                            errorVersionRemove,
                            errorVersionRemove
                        )
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            "$firstLine\n$secondLine\n$thirdLine",
                            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                        )
                    }
                    versionsToRemove = 0
                    versionsRemoved = 0
                    errorVersionRemove = 0
                }
            }
        } else if (request.type == MegaApiJava.USER_ATTR_AVATAR) {
            try {
                progressDialog?.first?.dismiss()
            } catch (ex: Exception) {
                Timber.e(ex)
            }
            if (e?.errorCode == MegaError.API_OK) {
                if (contactMail?.compareTo(request.email) == 0) {
                    val avatar = buildAvatarFile(this, "$contactMail.jpg")
                    if (FileUtil.isFileAvailable(avatar)) {
                        if ((avatar?.length() ?: 0) > 0) {
                            val bOpts = BitmapFactory.Options()
                            val bitmap = BitmapFactory.decodeFile(avatar?.absolutePath, bOpts)
                            if (bitmap == null) {
                                avatar?.delete()
                            } else {
                                bindingContent.contactListThumbnail.setImageBitmap(bitmap)
                                bindingContent.contactListThumbnail.isVisible = true
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Receive the result of requesting permissions
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        nodeSaver.handleRequestPermissionsResult(requestCode)
    }

    private fun onNodesUpdate(nodes: ArrayList<MegaNode>?) {
        Timber.d("onNodesUpdate")
        var thisNode = false
        var anyChild = false
        var updateContentFolder = false
        if (nodes == null) {
            return
        }
        var n: MegaNode? = null
        for (nodeToCheck in nodes) {
            if (nodeToCheck.handle == viewModel.node.handle) {
                thisNode = true
                n = nodeToCheck
                break
            } else {
                if (viewModel.node.isFolder) {
                    var parent = megaApi.getNodeByHandle(nodeToCheck.parentHandle)
                    while (parent != null) {
                        if (parent.handle == viewModel.node.handle) {
                            updateContentFolder = true
                            break
                        }
                        parent = megaApi.getNodeByHandle(parent.parentHandle)
                    }
                } else {
                    nodeVersions?.let { nodeVersions ->
                        for (j in nodeVersions.indices) {
                            if (nodeToCheck.handle == nodeVersions[j].handle) {
                                if (!anyChild) {
                                    anyChild = true
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
        if (updateContentFolder) {
            megaApi.getFolderInfo(viewModel.node, megaRequestListener)
        }
        if (!thisNode && !anyChild) {
            Timber.d("Not related to this node")
            return
        }

        //Check if the parent handle has changed
        if (n != null) {
            if (n.hasChanged(MegaNode.CHANGE_TYPE_PARENT)) {
                val oldParent = megaApi.getParentNode(viewModel.node)
                val newParent = megaApi.getParentNode(n)
                viewModel.updateNode(
                    if (oldParent.handle == newParent.handle) {
                        Timber.d("Parents match")
                        if (newParent.isFile) {
                            Timber.d("New version added")
                            newParent
                        } else {
                            n
                        }
                    } else {
                        n
                    }
                )
                nodeVersions = if (megaApi.hasVersions(viewModel.node)) {
                    megaApi.getVersions(viewModel.node)
                } else {
                    null
                }
            } else if (n.hasChanged(MegaNode.CHANGE_TYPE_REMOVED)) {
                if (thisNode) {
                    if (nodeVersions != null) {
                        val nodeHandle = nodeVersions?.getOrNull(1)?.handle ?: -1
                        if (megaApi.getNodeByHandle(nodeHandle) != null) {
                            viewModel.updateNode(megaApi.getNodeByHandle(nodeHandle))
                            nodeVersions = if (megaApi.hasVersions(viewModel.node)) {
                                megaApi.getVersions(viewModel.node)
                            } else {
                                null
                            }
                        } else {
                            finish()
                        }
                    } else {
                        finish()
                    }
                } else if (anyChild) {
                    nodeVersions = if (megaApi.hasVersions(n)) {
                        megaApi.getVersions(n)
                    } else {
                        null
                    }
                }
            } else {
                viewModel.updateNode(n)
                nodeVersions = if (megaApi.hasVersions(viewModel.node)) {
                    megaApi.getVersions(viewModel.node)
                } else {
                    null
                }
            }
        } else {
            if (anyChild) {
                nodeVersions = if (megaApi.hasVersions(viewModel.node)) {
                    megaApi.getVersions(viewModel.node)
                } else {
                    null
                }
            }
        }
        invalidateOptionsMenu()
        if (!viewModel.node.isTakenDown && viewModel.node.isExported) {
            Timber.d("Node HAS public link")
            publicLink = true
            bindingContent.dividerLinkLayout.isVisible = true
            bindingContent.filePropertiesLinkLayout.isVisible = true
            bindingContent.filePropertiesCopyLayout.isVisible = true
            bindingContent.filePropertiesLinkText.text = viewModel.node.publicLink
        } else {
            Timber.d("Node NOT public link")
            publicLink = false
            bindingContent.dividerLinkLayout.isVisible = false
            bindingContent.filePropertiesLinkLayout.isVisible = false
            bindingContent.filePropertiesCopyLayout.isVisible = false
        }
        if (viewModel.node.isFolder) {
            val sizeFile = megaApi.getSize(viewModel.node)
            bindingContent.filePropertiesInfoDataSize.text = Util.getSizeString(sizeFile)
            bindingContent.filePropertiesInfoDataContent.text =
                MegaApiUtils.getMegaNodeFolderInfo(viewModel.node)
            setIconResource()
            sl = megaApi.getOutShares(viewModel.node)
            sl?.let { sl ->
                if (sl.size == 0) {
                    Timber.d("sl.size == 0")
                    bindingContent.filePropertiesSharedLayout.isVisible = false
                    bindingContent.dividerSharedLayout.isVisible = false

                    //If I am the owner
                    if (megaApi.checkAccessErrorExtended(
                            viewModel.node,
                            MegaShare.ACCESS_OWNER
                        ).errorCode == MegaError.API_OK
                    ) {
                        binding.filePropertiesPermissionInfo.isVisible = false
                    } else {

                        //If I am not the owner
                        owner = false
                        binding.filePropertiesPermissionInfo.isVisible = true
                        val accessLevel = megaApi.getAccess(viewModel.node)
                        Timber.d("Node: %s", viewModel.node.handle)
                        when (accessLevel) {
                            MegaShare.ACCESS_OWNER, MegaShare.ACCESS_FULL -> {
                                binding.filePropertiesPermissionInfo.text =
                                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_full_access)
                                        .uppercase(Locale.getDefault())
                            }
                            MegaShare.ACCESS_READ -> {
                                binding.filePropertiesPermissionInfo.text =
                                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_only)
                                        .uppercase(Locale.getDefault())
                            }
                            MegaShare.ACCESS_READWRITE -> {
                                binding.filePropertiesPermissionInfo.text =
                                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_write)
                                        .uppercase(Locale.getDefault())
                            }
                        }
                    }
                } else {
                    bindingContent.filePropertiesSharedLayout.isVisible = true
                    bindingContent.dividerSharedLayout.isVisible = true
                    bindingContent.filePropertiesSharedInfoButton.text =
                        getQuantityStringOrDefault(
                            R.plurals.general_selection_num_contacts,
                            sl.size, sl.size
                        )
                }
            }
        } else {
            bindingContent.filePropertiesInfoDataSize.text = Util.getSizeString(viewModel.node.size)
        }
        if (viewModel.node.creationTime != 0L) {
            try {
                bindingContent.filePropertiesInfoDataAdded.text =
                    DateUtils.getRelativeTimeSpanString(viewModel.node.creationTime * 1000)
            } catch (ex: Exception) {
                bindingContent.filePropertiesInfoDataAdded.text = ""
            }
            if (viewModel.node.modificationTime != 0L) {
                try {
                    bindingContent.filePropertiesInfoDataCreated.text =
                        DateUtils.getRelativeTimeSpanString(
                            viewModel.node.modificationTime * 1000
                        )
                } catch (ex: Exception) {
                    bindingContent.filePropertiesInfoDataCreated.text = ""
                }
            } else {
                try {
                    bindingContent.filePropertiesInfoDataCreated.text =
                        DateUtils.getRelativeTimeSpanString(
                            viewModel.node.creationTime * 1000
                        )
                } catch (ex: Exception) {
                    bindingContent.filePropertiesInfoDataCreated.text = ""
                }
            }
        } else {
            bindingContent.filePropertiesInfoDataAdded.text = ""
            bindingContent.filePropertiesInfoDataCreated.text = ""
        }

        refresh()
    }


    /**
     * Perform final cleaning when the activity is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        megaApi.removeGlobalListener(megaGlobalListener)
        megaApi.removeRequestListener(megaRequestListener)
        unregisterReceiver(contactUpdateReceiver)
        unregisterReceiver(manageShareReceiver)
        nodeSaver.destroy()
        progressDialog?.first?.dismiss()
    }


    /**
     * Called to retrieve per-instance state from an activity before being killed so that the state can be restored
     */
    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (selectedShare != null) {
            outState.putLong(KEY_SELECTED_SHARE_HANDLE, selectedShare?.nodeHandle ?: -1)
        }
        nodeAttacher.saveState(outState)
        nodeSaver.saveState(outState)
    }

    /**
     * receive the click of an item from the adapter
     */
    fun itemClick(position: Int) {
        Timber.d("Position: %s", position)
        if (adapter.isMultipleSelect) {
            adapter.toggleSelection(position)
            updateActionModeTitle()
        } else {
            val megaUser = listContacts[position].user
            val contact = megaApi.getContact(megaUser)
            if (contact != null && contact.visibility == MegaUser.VISIBILITY_VISIBLE) {
                ContactUtil.openContactInfoActivity(this, megaUser)
            }
        }
    }

    /**
     * receive the show options panel action from the adapter
     */
    fun showOptionsPanel(sShare: MegaShare?) {
        Timber.d("showNodeOptionsPanel")
        if (sShare == null || bottomSheetDialogFragment.isBottomSheetDialogShown()) return
        selectedShare = sShare
        bottomSheetDialogFragment =
            FileContactsListBottomSheetDialogFragment(
                selectedShare,
                selectedContact,
                viewModel.node
            )
        bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
    }

    /**
     * hides the multi select option
     */
    fun hideMultipleSelect() {
        adapter.isMultipleSelect = false
        actionMode?.finish()
    }

    private val selectedContact: MegaUser?
        get() {
            val email = selectedShare?.user
            return megaApi.getContact(email)
        }

    /**
     * Receive the change permissions action to show the different permission options
     */
    fun changePermissions() {
        Timber.d("changePermissions")
        val dialogBuilder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        dialogBuilder.setTitle(getFormattedStringOrDefault(R.string.file_properties_shared_folder_permissions))
        val items = arrayOf<CharSequence>(
            getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_only),
            getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_write),
            getFormattedStringOrDefault(R.string.file_properties_shared_folder_full_access)
        )
        val selected = selectedShare ?: return
        dialogBuilder.setSingleChoiceItems(items, selected.access) { _, item ->
            showProgressDialog(
                getFormattedStringOrDefault(R.string.context_permissions_changing_folder)
            )
            permissionsDialog?.dismiss()
            cC?.changePermission(
                selected.user,
                item,
                viewModel.node,
                ShareListener(this, ShareListener.CHANGE_PERMISSIONS_LISTENER, 1)
            )
        }
        permissionsDialog = dialogBuilder.show()
    }

    /**
     * Receive remove contact action to show a confirmation dialog
     */
    fun removeFileContactShare(): AlertDialog =
        showConfirmationRemoveContactFromShare(selectedShare?.user)

    private fun showConfirmationRemoveContactFromShare(email: String?) =
        MaterialAlertDialogBuilder(this)
            .setMessage(getFormattedStringOrDefault(R.string.remove_contact_shared_folder, email))
            .setPositiveButton(R.string.general_remove) { _: DialogInterface?, _: Int ->
                removeShare(email)
            }
            .setNegativeButton(R.string.general_cancel, null)
            .show()

    private fun removeShare(email: String?) {
        showProgressDialog(
            getFormattedStringOrDefault(R.string.context_removing_contact_folder)
        )
        nodeController.removeShare(
            ShareListener(
                this,
                ShareListener.REMOVE_SHARE_LISTENER,
                1
            ), viewModel.node, email
        )
    }

    private fun refresh() {
        setContactList()
        setMoreButtonText()
        adapter.setShareList(listContacts)
    }

    private fun setContactList() {
        fullListContacts = megaApi.getOutShares(viewModel.node)
        listContacts = fullListContacts.take(MAX_NUMBER_OF_CONTACTS_IN_LIST)
    }

    @SuppressLint("SetTextI18n")
    private fun setMoreButtonText() {
        val fullSize = fullListContacts.size
        with(bindingContent.moreButton) {
            if (fullSize > MAX_NUMBER_OF_CONTACTS_IN_LIST) {
                isVisible = true
                text =
                    "${(fullSize - MAX_NUMBER_OF_CONTACTS_IN_LIST)} ${getFormattedStringOrDefault(R.string.label_more)}"
            } else {
                isVisible = false
            }
        }

    }

    private fun showConfirmationRemoveMultipleContactFromShare(contacts: ArrayList<MegaShare>) =
        MaterialAlertDialogBuilder(this)
            .setMessage(
                getQuantityStringOrDefault(
                    R.plurals.remove_multiple_contacts_shared_folder,
                    contacts.size,
                    contacts.size
                )
            )
            .setPositiveButton(getFormattedStringOrDefault(R.string.general_remove)) { _: DialogInterface?, _: Int ->
                removeMultipleShares(contacts)
            }
            .setNegativeButton(R.string.general_cancel, null)
            .show()

    private fun removeMultipleShares(shares: ArrayList<MegaShare>?) {
        Timber.d("removeMultipleShares")
        showProgressDialog(
            getFormattedStringOrDefault(R.string.context_removing_contact_folder)
        )
        nodeController.removeShares(shares, viewModel.node)
    }

    // Clear all selected items
    private fun clearSelections() {
        if (adapter.isMultipleSelect) {
            adapter.clearSelections()
        }
    }

    private fun selectAll() {
        Timber.d("selectAll")
        if (adapter.isMultipleSelect) {
            adapter.selectAll()
        } else {
            adapter.isMultipleSelect = true
            adapter.selectAll()
            actionMode = startSupportActionMode(ActionBarCallBack())
        }
        Handler(Looper.getMainLooper()).post { updateActionModeTitle() }
    }

    private fun updateAdapter(handleReceived: Long) {
        for (i in listContacts.indices) {
            val email = listContacts[i].user
            val contact = megaApi.getContact(email)
            val handleUser = contact.handle
            if (handleUser == handleReceived) {
                adapter.notifyItemChanged(i)
                break
            }
        }
    }

    private fun updateActionModeTitle() {
        Timber.d("updateActionModeTitle")
        if (actionMode == null) {
            return
        }
        val contacts: List<MegaShare> = adapter.selectedShares ?: emptyList()
        Timber.d("Contacts selected: %s", contacts.size)
        actionMode?.title = getQuantityStringOrDefault(
            R.plurals.general_selection_num_contacts,
            contacts.size, contacts.size
        )
        try {
            actionMode?.invalidate()
        } catch (e: NullPointerException) {
            Timber.e(e, "Invalidate error")
        }
    }

    private fun setIconResource() {
        val resource = if (viewModel.node.isFolder) {
            getFolderIcon(
                viewModel.node,
                if (adapterType == Constants.OUTGOING_SHARES_ADAPTER) DrawerItem.SHARED_ITEMS else DrawerItem.CLOUD_DRIVE
            )
        } else {
            MimeTypeThumbnail.typeForName(viewModel.node.name).iconResourceId
        }
        binding.fileInfoToolbarIcon.setImageResource(resource)
    }

    companion object {
        private const val MAX_NUMBER_OF_CONTACTS_IN_LIST = 5
        private const val MAX_WIDTH_FILENAME_LAND = 400f
        private const val MAX_WIDTH_FILENAME_LAND_2 = 400f
        private const val MAX_WIDTH_FILENAME_PORT = 170f
        private const val MAX_WIDTH_FILENAME_PORT_2 = 200f

        /**
         * key to return the handle to the calling activity
         */
        const val NODE_HANDLE = "NODE_HANDLE"

        /**
         * remove type
         */
        @JvmField
        var TYPE_EXPORT_REMOVE = 1
        private const val KEY_SELECTED_SHARE_HANDLE = "KEY_SELECTED_SHARE_HANDLE"
    }
}