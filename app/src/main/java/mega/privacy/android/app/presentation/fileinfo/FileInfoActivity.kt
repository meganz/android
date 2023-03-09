package mega.privacy.android.app.presentation.fileinfo

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spanned
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.activities.contract.DeleteVersionsHistoryActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToCopyActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToMoveActivityContract
import mega.privacy.android.app.activities.contract.SelectUsersToShareActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.databinding.ActivityFileInfoBinding
import mega.privacy.android.app.databinding.DialogLinkBinding
import mega.privacy.android.app.interfaces.ActionBackupListener
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
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.extensions.iconRes
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.sync.fileBackups.FileBackupManager
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_EXECUTE
import mega.privacy.android.app.usecase.data.MoveRequestResult
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CameraUploadUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.TAKEDOWN_URL
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeByHandle
import mega.privacy.android.app.utils.MegaNodeUtil.getFolderIcon
import mega.privacy.android.app.utils.MegaNodeUtil.handleLocationClick
import mega.privacy.android.app.utils.MegaNodeUtil.isEmptyFolder
import mega.privacy.android.app.utils.MegaNodeUtil.showConfirmationLeaveIncomingShare
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaUser
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Activity for showing file and folder info.
 *
 * @property passCodeFacade [PasscodeCheck] an injected component to enforce a Passcode security check
 */
@AndroidEntryPoint
class FileInfoActivity : BaseActivity(), SnackbarShower {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    private val viewModel: FileInfoViewModel by viewModels()

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

    private var typeExport = -1
    private var mOffDelete: MegaOffline? = null
    private var availableOfflineBoolean = false
    private var cC: ContactController? = null
    private var progressDialog: Pair<AlertDialog, FileInfoJobInProgressState?>? = null
    private var publicLink = false
    private val density by lazy { outMetrics.density }
    private var shareIt = true
    private var from = 0
    private var permissionsDialog: AlertDialog? = null
    private var isRemoveOffline = false
    private var handle: Long = 0
    private var adapterType = 0
    private var selectedShare: MegaShare? = null
    private val adapter: MegaFileInfoSharedContactAdapter by lazy {
        MegaFileInfoSharedContactAdapter(
            this,
            viewModel.node,
            emptyList(),
            bindingContent.fileInfoContactListView
        ).apply {
            positionClicked = -1
            isMultipleSelect = false
        }
    }
    private var actionMode: ActionMode? = null
    private var bottomSheetDialogFragment: FileContactsListBottomSheetDialogFragment? = null
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
                        setTitle(getString(R.string.file_properties_shared_folder_permissions))

                        val items = arrayOf<CharSequence>(
                            getString(R.string.file_properties_shared_folder_read_only),
                            getString(R.string.file_properties_shared_folder_read_write),
                            getString(R.string.file_properties_shared_folder_full_access)
                        )

                        setSingleChoiceItems(items, -1) { _, it ->
                            clearSelections()
                            permissionsDialog?.dismiss()
                            showProgressDialog(
                                getString(R.string.context_permissions_changing_folder)
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
                unselect.title = getString(R.string.action_unselect_all)
                unselect.isVisible = true
            } else {
                menu.findItem(R.id.cab_menu_select_all).isVisible = true
                menu.findItem(R.id.cab_menu_unselect_all).isVisible = false
            }
            val changePermissionsMenuItem = menu.findItem(R.id.action_file_contact_list_permissions)
            if (viewModel.uiState.value.isNodeInInbox) {
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
        viewModel.tempInit(getNodeFromExtras() ?: run {
            finish()
            return
        })
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        initFileBackupManager()
        cC = ContactController(this)
        adapterType = intent.getIntExtra("adapterType", Constants.FILE_BROWSER_ADAPTER)

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
        menuHelper.setPreview(viewState.actualPreviewUriString)
        updateFileHistoryVersions(viewState.showHistoryVersions, viewState.historyVersions)
        viewState.sizeString?.let { updateSize(it) }
        updateFolderContentInfo(
            viewState.folderTreeInfo != null,
            viewState.folderContentInfoString
        )
        updateFolderHistoryVersions(
            show = viewState.showFolderHistoryVersions,
            numVersions = viewState.folderTreeInfo?.numberOfVersions ?: 0,
            currentVersionsSize = viewState.folderCurrentVersionSizeInBytesString,
            previousVersionsSize = viewState.folderVersionsSizeInBytesString
        )
        updateAvailableOffline(!viewModel.node.isTakenDown && !viewState.isNodeInRubbish)
        updateIncomeShared(viewState)
        updateOutShares(viewState)
        updateLocation(viewState.nodeLocationInfo)

        refreshProperties()
    }

    private fun updateLocation(locationInfo: LocationInfo?) {
        with(bindingContent.filePropertiesInfoDataLocation) {
            isVisible = locationInfo != null
            text = locationInfo?.location
        }
    }

    private fun updateIncomeShared(viewState: FileInfoViewState) {
        bindingContent.filePropertiesOwnerLayout.isVisible = viewState.isIncomingSharedNode
        viewState.inShareOwnerContactItem?.let { contactItem ->
            bindingContent.filePropertiesOwnerLabel.text = viewState.ownerLabel
            bindingContent.filePropertiesOwnerInfo.text = contactItem.email
            bindingContent.filePropertiesOwnerStateIcon.setImageResource(
                contactItem.status.iconRes(isLightTheme = !Util.isDarkMode(this))
            )

            val defaultAvatar = BitmapDrawable(
                resources,
                AvatarUtil.getDefaultAvatar(
                    contactItem.defaultAvatarColor.toColorInt(),
                    contactItem.getAvatarFirstLetter(),
                    Constants.AVATAR_SIZE,
                    true
                )
            )
            bindingContent.contactListThumbnail.hierarchy.setFailureImage(defaultAvatar)
            bindingContent.contactListThumbnail.setImageURI(
                File(contactItem.contactData.avatarUri ?: "").toUri(),
                null,
            )

            viewState.accessPermission.description()?.let {
                binding.filePropertiesPermissionInfo.setText(it)
                binding.filePropertiesPermissionInfo.isVisible = true
            } ?: run {
                binding.filePropertiesPermissionInfo.isVisible = false
            }
        }
    }

    private fun updateFileHistoryVersions(show: Boolean, versions: Int) = with(bindingContent) {
        // If the Node belongs to Backups or has no versions, then hide
        // the Versions layout
        filePropertiesVersionsLayout.isVisible = show
        separatorVersions.isVisible = show
        if (show) {
            val text = resources.getQuantityString(R.plurals.number_of_versions, versions, versions)
            filePropertiesTextNumberVersions.text = text
        }
    }

    private fun updateSize(size: String) {
        bindingContent.filePropertiesInfoDataSize.text = size
    }

    private fun updateFolderContentInfo(show: Boolean, folderContentInfo: String) {
        bindingContent.filePropertiesContentLayout.isVisible = show
        bindingContent.filePropertiesInfoDataContent.text = folderContentInfo
    }

    private fun updateFolderHistoryVersions(
        show: Boolean,
        numVersions: Int,
        currentVersionsSize: String,
        previousVersionsSize: String,
    ) = with(bindingContent) {
        bindingContent.filePropertiesFolderVersionsLayout.isVisible = show
        bindingContent.filePropertiesFolderCurrentVersionsLayout.isVisible = show
        bindingContent.filePropertiesFolderPreviousVersionsLayout.isVisible = show
        if (show) {
            val text = resources.getQuantityString(
                R.plurals.number_of_versions_inside_folder,
                numVersions,
                numVersions
            )
            bindingContent.filePropertiesInfoDataFolderVersions.text = text
            bindingContent.filePropertiesInfoDataFolderCurrentVersions.text = currentVersionsSize
            bindingContent.filePropertiesInfoDataFolderPreviousVersions.text = previousVersionsSize
        }
    }

    private fun updateAvailableOffline(enabled: Boolean) = with(bindingContent) {
        if (enabled) {
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

    private fun updateOptionsMenu(state: FileInfoViewState) {
        if (state.jobInProgressState == null) {
            menuHelper.setNodeName(state.title)
            menuHelper.updateOptionsMenu(
                node = viewModel.node,
                isInInbox = state.isNodeInInbox,
                isInRubbish = state.isNodeInRubbish,
                fromIncomingShares = from == Constants.FROM_INCOMING_SHARES,
                firstIncomingLevel = firstIncomingLevel,
                nodeAccess = state.accessPermission,
            )
        } else {
            menuHelper.disableMenu()
        }
    }

    private fun consumeEvent(event: FileInfoOneOffViewEvent) {
        when (event) {
            FileInfoOneOffViewEvent.NotConnected -> {
                Util.showErrorAlertDialog(
                    getString(R.string.error_server_connection_problem),
                    false,
                    this
                )
            }
            FileInfoOneOffViewEvent.NodeDeleted -> {
                //the node has been deleted, this screen has no more sense
                finish()
            }
            is FileInfoOneOffViewEvent.GeneralError -> showSnackBar(R.string.general_error)
            is FileInfoOneOffViewEvent.CollisionDetected -> {
                val list = ArrayList<NameCollision>()
                list.add(event.collision)
                nameCollisionActivityContract?.launch(list)
            }
            is FileInfoOneOffViewEvent.Finished -> {
                if (event.exception == null) {
                    showSnackBar(event.jobFinished.successMessage)
                    sendBroadcast(Intent(Constants.BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN))
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

    private fun showSnackBar(@StringRes resString: Int?) =
        resString?.let { showSnackBar(getString(it)) }

    private fun showSnackBar(message: String?) = message?.let {
        showSnackbar(
            Constants.SNACKBAR_TYPE,
            it,
            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
        )
    }

    private fun updateProgress(progressState: FileInfoJobInProgressState?) {
        if (progressState == null) {
            progressDialog?.first?.dismiss()
            progressDialog = null
        } else if (progressDialog?.second != progressState) {
            progressState.progressMessage?.let {
                showProgressDialog(getString(it), progressState)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateOutShares(viewState: FileInfoViewState) {
        val visibleOutShares = viewState.outSharesCoerceMax
        bindingContent.filePropertiesSharedLayout.isVisible = visibleOutShares.isNotEmpty()
        bindingContent.dividerSharedLayout.isVisible = visibleOutShares.isNotEmpty()
        bindingContent.sharedContactListContainer.isVisible =
            visibleOutShares.isNotEmpty() && viewState.isShareContactExpanded
        if (visibleOutShares.isNotEmpty()) {
            if (adapter.isMultipleSelect) {
                adapter.clearSelections()
                hideMultipleSelect()
            }
            adapter.setShareList(visibleOutShares)
            //set more button text
            with(bindingContent.moreButton) {
                if (viewModel.uiState.value.thereAreMoreOutShares) {
                    isVisible = true
                    text =
                        "${viewModel.uiState.value.extraOutShares} ${getString(R.string.label_more)}"
                } else {
                    isVisible = false
                }
            }
            //shared with and permission
            bindingContent.filePropertiesSharedInfoButton.text =
                resources.getQuantityString(
                    R.plurals.general_selection_num_contacts,
                    visibleOutShares.size, visibleOutShares.size
                )
            //shared with expanded or not
            if (viewState.isShareContactExpanded) {
                bindingContent.filePropertiesSharedInfoButton.setText(R.string.general_close)
            } else {
                viewModel.uiState.value.outShares.let { sl ->
                    bindingContent.filePropertiesSharedInfoButton.text =
                        resources.getQuantityString(
                            R.plurals.general_selection_num_contacts,
                            sl.size, sl.size
                        )
                }
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
        return if (extras != null) {
            from = extras.getInt("from")
            if (from == Constants.FROM_INCOMING_SHARES) {
                firstIncomingLevel = extras.getBoolean(Constants.INTENT_EXTRA_KEY_FIRST_LEVEL)
            }
            val handleNode = extras.getLong("handle", -1)
            Timber.d("Handle of the selected node: %s", handleNode)
            megaApi.getNodeByHandle(handleNode)

        } else {
            Timber.w("Extras is NULL")
            null
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
        }

        with(bindingContent) {
            //Available Offline Layout
            availableOfflineLayout.isVisible = true

            //Share with Layout
            filePropertiesSharedLayout.setOnClickListener {
                viewModel.expandOutSharesClick()
            }
            filePropertiesSharedInfoButton.setOnClickListener {
                viewModel.expandOutSharesClick()
            }
            //Owner Layout
            val ownerString = "(${getString(R.string.file_properties_owner)})"
            filePropertiesOwnerLabelOwner.text = ownerString
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                filePropertiesOwnerLabel.setMaxWidthEmojis(Util.dp2px(MAX_WIDTH_FILENAME_LAND))
                filePropertiesOwnerInfo.maxWidth = Util.dp2px(MAX_WIDTH_FILENAME_LAND_2)
            } else {
                filePropertiesOwnerLabel.setMaxWidthEmojis(Util.dp2px(MAX_WIDTH_FILENAME_PORT))
                filePropertiesOwnerInfo.maxWidth = Util.dp2px(MAX_WIDTH_FILENAME_PORT_2)
            }

            //Folder Versions Layout
            filePropertiesFolderVersionsLayout.isVisible = false
            filePropertiesFolderCurrentVersionsLayout.isVisible = false
            filePropertiesFolderPreviousVersionsLayout.isVisible = false

            //Content Layout
            filePropertiesLinkButton.text = getString(R.string.context_copy)
            filePropertiesLinkButton.setOnClickListener {
                Timber.d("Copy link button")
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Text", viewModel.node.publicLink)
                clipboard.setPrimaryClip(clip)
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.file_properties_get_link),
                    -1
                )
            }

            val listView = fileInfoContactListView
            listView.itemAnimator = Util.noChangeRecyclerViewItemAnimator()
            listView.addItemDecoration(SimpleDividerItemDecoration(this@FileInfoActivity))
            val mLayoutManager = LinearLayoutManager(this@FileInfoActivity)
            listView.layoutManager = mLayoutManager

            moreButton.setOnClickListener {
                val i = Intent(this@FileInfoActivity, FileContactListActivity::class.java)
                i.putExtra(Constants.NAME, viewModel.node.handle)
                startActivity(i)
            }

            //setup adapter
            listView.adapter = adapter

            filePropertiesInfoDataLocation.setOnClickListener {
                viewModel.uiState.value.nodeLocationInfo?.let { locationInfo ->
                    handleLocationClick(this@FileInfoActivity, adapterType, locationInfo)
                }
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
                        getString(R.string.context_remove_link_warning_text)

                    val isLandscape =
                        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    val size = if (isLandscape) 10 else 15
                    val scaleW = Util.getScaleW(outMetrics, density)
                    dialogLinkTextRemove.setTextSize(TypedValue.COMPLEX_UNIT_SP, size * scaleW)
                }

                MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                    .setView(dialogLayout.root)
                    .setPositiveButton(getString(R.string.context_remove)) { _: DialogInterface?, _: Int ->
                        typeExport = TYPE_EXPORT_REMOVE
                        megaApi.disableExport(viewModel.node)
                    }.setNegativeButton(getString(R.string.general_cancel), null)
                    .show()
            }
            R.id.cab_menu_file_info_copy -> copyLauncher.launch(longArrayOf(viewModel.node.handle))
            R.id.cab_menu_file_info_move -> moveLauncher.launch(longArrayOf(viewModel.node.handle))
            R.id.cab_menu_file_info_rename -> {
                showRenameNodeDialog(this, viewModel.node, this, null)
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
            R.id.cab_menu_file_info_dispute -> {
                startActivity(
                    Intent(this, WebViewActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .setData(Uri.parse(Constants.DISPUTE_URL))
                )
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
                                getString(R.string.file_properties_shared_folder_read_only),
                                getString(R.string.file_properties_shared_folder_read_write),
                                getString(R.string.file_properties_shared_folder_full_access)
                            )
                            permissionsDialog =
                                MaterialAlertDialogBuilder(
                                    this,
                                    R.style.ThemeOverlay_Mega_MaterialAlertDialog
                                )
                                    .setTitle(getString(R.string.file_properties_shared_folder_permissions))
                                    .setSingleChoiceItems(items, -1) { _, item ->
                                        showProgressDialog(
                                            getString(R.string.context_sharing_folder)
                                        )
                                        permissionsDialog?.dismiss()
                                        nodeController.shareFolder(
                                            megaApi.getNodeByHandle(viewModel.node.handle),
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
                    viewModel.deleteHistoryVersions()
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
     * Listen an propagate results to node saver.
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, data)) {
            return
        }
    }

    /**
     * Starts a new Intent to share the folder to different contacts
     */
    private fun shareFolder() {
        selectContactForShareFolderLauncher.launch(viewModel.node)
    }

    private fun refreshProperties() {
        Timber.d("refreshProperties")
        if (!viewModel.node.isTakenDown && viewModel.node.isExported) {
            publicLink = true
            bindingContent.dividerLinkLayout.isVisible = true
            bindingContent.filePropertiesLinkLayout.isVisible = true
            bindingContent.filePropertiesCopyLayout.isVisible = true
            bindingContent.filePropertiesLinkText.text = viewModel.node.publicLink
            bindingContent.filePropertiesLinkDate.text = getString(
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
                getString(R.string.file_properties_info_size_file)
            bindingContent.filePropertiesInfoDataSize.text = Util.getSizeString(viewModel.node.size)
            if (viewModel.node.isTakenDown) {
                bindingContent.takenDownFileWarningBanner.text =
                    underlineText(R.string.cloud_drive_info_taken_down_file_warning)
                bindingContent.takenDownFileWarningBanner.setOnClickListener {
                    redirectToTakedownPolicy()
                }
                bindingContent.warningBannerLayout.isVisible = true
                bindingContent.takenDownFileWarningClose.setOnClickListener {
                    bindingContent.warningBannerLayout.isVisible = false
                }
            }
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
        } else if (viewModel.node.isFolder) {
            Timber.d("Node is FOLDER")
            setIconResource()
            if (viewModel.node.isTakenDown) {
                bindingContent.takenDownFileWarningBanner.text =
                    underlineText(R.string.cloud_drive_info_taken_down_folder_warning)
                bindingContent.takenDownFileWarningBanner.setOnClickListener {
                    redirectToTakedownPolicy()
                }
                bindingContent.warningBannerLayout.isVisible = true
                bindingContent.takenDownFileWarningClose.setOnClickListener {
                    bindingContent.warningBannerLayout.isVisible = false
                }
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

    private fun filePropertiesSwitch() {
        val isChecked = bindingContent.filePropertiesSwitch.isChecked
        if (viewModel.getStorageState() === StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            bindingContent.filePropertiesSwitch.isChecked = !isChecked
            return
        }
        if (!viewModel.uiState.value.isIncomingSharedNode) {
            Timber.d("Owner: me")
            if (!isChecked) {
                Timber.d("isChecked")
                isRemoveOffline = true
                handle = viewModel.node.handle
                availableOfflineBoolean = false
                bindingContent.filePropertiesSwitch.isChecked = false
                mOffDelete = dbH.findByHandle(handle)
                OfflineUtils.removeOffline(mOffDelete, dbH, this)
                showSnackbar(getString(R.string.file_removed_offline))
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

    /**
     * Perform final cleaning when the activity is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
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
            val megaUser = viewModel.uiState.value.outShares.getOrNull(position)?.user
            val contact = megaUser?.let { megaApi.getContact(it) }
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
        dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions))
        val items = arrayOf<CharSequence>(
            getString(R.string.file_properties_shared_folder_read_only),
            getString(R.string.file_properties_shared_folder_read_write),
            getString(R.string.file_properties_shared_folder_full_access)
        )
        val selected = selectedShare ?: return
        dialogBuilder.setSingleChoiceItems(items, selected.access) { _, item ->
            showProgressDialog(
                getString(R.string.context_permissions_changing_folder)
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
            .setMessage(getString(R.string.remove_contact_shared_folder, email))
            .setPositiveButton(R.string.general_remove) { _: DialogInterface?, _: Int ->
                removeShare(email)
            }
            .setNegativeButton(R.string.general_cancel, null)
            .show()

    private fun removeShare(email: String?) {
        showProgressDialog(
            getString(R.string.context_removing_contact_folder)
        )
        nodeController.removeShare(
            ShareListener(
                this,
                ShareListener.REMOVE_SHARE_LISTENER,
                1
            ), viewModel.node, email
        )
    }

    private fun showConfirmationRemoveMultipleContactFromShare(contacts: ArrayList<MegaShare>) =
        MaterialAlertDialogBuilder(this)
            .setMessage(
                resources.getQuantityString(
                    R.plurals.remove_multiple_contacts_shared_folder,
                    contacts.size,
                    contacts.size
                )
            )
            .setPositiveButton(getString(R.string.general_remove)) { _: DialogInterface?, _: Int ->
                removeMultipleShares(contacts)
            }
            .setNegativeButton(R.string.general_cancel, null)
            .show()

    private fun removeMultipleShares(shares: ArrayList<MegaShare>?) {
        Timber.d("removeMultipleShares")
        showProgressDialog(
            getString(R.string.context_removing_contact_folder)
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

    private fun updateActionModeTitle() {
        Timber.d("updateActionModeTitle")
        if (actionMode == null) {
            return
        }
        val contacts: List<MegaShare> = adapter.selectedShares ?: emptyList()
        Timber.d("Contacts selected: %s", contacts.size)
        actionMode?.title = resources.getQuantityString(
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

    private fun redirectToTakedownPolicy() {
        val uriUrl = Uri.parse(TAKEDOWN_URL)
        val launchBrowser = Intent(this, WebViewActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setData(uriUrl)
        startActivity(launchBrowser)
    }

    private fun underlineText(@StringRes res: Int): Spanned {
        return getString(res)
            .replace("[A]", "<u>")
            .replace("[/A]", "</u>")
            .toSpannedHtmlText()
    }

    companion object {
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