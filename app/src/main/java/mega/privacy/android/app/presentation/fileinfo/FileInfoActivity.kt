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
import mega.privacy.android.app.main.FileContactListActivity
import mega.privacy.android.app.main.adapters.MegaFileInfoSharedContactAdapter
import mega.privacy.android.app.main.adapters.MegaFileInfoSharedContactAdapter.MegaFileInfoSharedContactAdapterListener
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.extensions.iconRes
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoJobInProgressState
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoOneOffViewEvent
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoOrigin
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.sync.fileBackups.FileBackupManager
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_EXECUTE
import mega.privacy.android.app.usecase.data.MoveRequestResult
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CameraUploadUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.FROM_INCOMING_SHARES
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FIRST_LEVEL
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FROM
import mega.privacy.android.app.utils.Constants.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.TAKEDOWN_URL
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeByHandle
import mega.privacy.android.app.utils.MegaNodeUtil.handleLocationClick
import mega.privacy.android.app.utils.MegaNodeUtil.showConfirmationLeaveIncomingShare
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaUser
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
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
            if (viewModel.uiState.value.isAvailableOffline.not()) {
                val intent = Intent()
                intent.putExtra(NODE_HANDLE, viewModel.nodeId.longValue)
                setResult(RESULT_OK, intent)
            }
            finish()
        }
    }

    private lateinit var binding: ActivityFileInfoBinding
    private val bindingContent get() = binding.contentFileInfoActivity
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
            nestedView = bindingContent.nestedLayout
        )
    }

    private var progressDialog: Pair<AlertDialog, FileInfoJobInProgressState?>? = null
    private val density by lazy { outMetrics.density }
    private var adapterType = 0
    private val adapter: MegaFileInfoSharedContactAdapter by lazy {
        MegaFileInfoSharedContactAdapter(
            this,
            viewModel.node,
            emptyList(),
            bindingContent.fileInfoContactListView,
            object : MegaFileInfoSharedContactAdapterListener {
                override fun itemClick(currentPosition: Int) {
                    Timber.d("Position: %s", currentPosition)
                    if (adapter.isMultipleSelect) {
                        adapter.toggleSelection(currentPosition)
                        updateActionModeTitle()
                    } else {
                        val megaUser =
                            viewModel.uiState.value.outShares.getOrNull(currentPosition)?.user
                        val contact = megaUser?.let { megaApi.getContact(it) }
                        if (contact != null && contact.visibility == MegaUser.VISIBILITY_VISIBLE) {
                            ContactUtil.openContactInfoActivity(this@FileInfoActivity, megaUser)
                        }
                    }
                }

                override fun showOptionsPanel(share: MegaShare?) {
                    viewModel.contactSelectedToShowOptions(share)
                }

                override fun hideMultipleSelect() {
                    hideMultipleSelectOfSharedContacts()
                }

                override fun activateActionMode() {
                    Timber.d("activateActionMode")
                    if (!adapter.isMultipleSelect) {
                        adapter.isMultipleSelect = true
                        actionMode = startSupportActionMode(ActionBarCallBack())
                    }
                }

            }
        ).apply {
            positionClicked = -1
            isMultipleSelect = false
        }
    }
    private var actionMode: ActionMode? = null
    private var bottomSheetDialogFragment: FileContactsListBottomSheetDialogFragment? = null
    private lateinit var selectContactForShareFolderLauncher: ActivityResultLauncher<NodeId>
    private lateinit var versionHistoryLauncher: ActivityResultLauncher<Long>
    private lateinit var copyLauncher: ActivityResultLauncher<LongArray>
    private lateinit var moveLauncher: ActivityResultLauncher<LongArray>

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
                    showAccessPermissionOptions { permission ->
                        //once the adapter is removed in favour of compose LazyColumn, this won't be needed
                        viewModel.contactsSelectedInSharedList(shares.map { it.user })
                        viewModel.setSharePermissionForCurrentSelectedList(permission.toAccessPermission())
                        clearSelections()
                    }
                }
                R.id.action_file_contact_list_delete -> {
                    Timber.d("Remove  contacts")
                    showConfirmationRemoveContactsFromShare(*shares.map { it.user }.toTypedArray())
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
        }, getOriginFromExtras())
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        initFileBackupManager()
        adapterType =
            intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.FILE_BROWSER_ADAPTER)

        savedInstanceState?.apply {
            nodeAttacher.restoreState(this)
            nodeSaver.restoreState(this)
        }

        //setup view
        setupView()
        setupViewListeners()

        //update view
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
        updateSize(viewState.sizeInBytes)
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
        updateAvailableOffline(
            visible = viewState.isAvailableOfflineAvailable,
            enabled = viewState.isAvailableOfflineEnabled,
            available = viewState.isAvailableOffline,
        )
        updateIncomeShared(viewState)
        updateOutShares(viewState)
        updateLocation(viewState.nodeLocationInfo)
        updateContactShareBottomSheet(viewState)
        updateCreationTime(viewState)
        updateIcon(viewState)
        updateProperties(viewState)
        updateTakenDown(
            isTakenDown = viewState.isTakenDown,
            isFile = viewState.isFile
        )
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

    private fun updateSize(size: Long) {
        bindingContent.filePropertiesInfoDataSize.text = Util.getSizeString(size)
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

    private fun updateAvailableOffline(visible: Boolean, enabled: Boolean, available: Boolean) =
        with(bindingContent) {
            bindingContent.availableOfflineLayout.isVisible = visible
            bindingContent.availableOfflineSeparator.isVisible = visible
            if (visible) {
                bindingContent.filePropertiesSwitch.isChecked = available
                filePropertiesSwitch.isEnabled = enabled
                filePropertiesAvailableOfflineText.setTextColor(
                    ContextCompat.getColor(
                        this@FileInfoActivity,
                        if (enabled) R.color.grey_087_white_087 else R.color.grey_700_026_grey_300_026
                    )
                )
            }
        }

    private fun updateContactShareBottomSheet(viewState: FileInfoViewState) =
        viewState.outShareContactShowOptions?.takeIf {
            viewState.jobInProgressState !is FileInfoJobInProgressState.ChangeSharePermission &&
                    !bottomSheetDialogFragment.isBottomSheetDialogShown()
        }?.let { share ->
            Timber.d("showNodeOptionsPanel")
            bottomSheetDialogFragment =
                FileContactsListBottomSheetDialogFragment(share, viewModel.node)
            bottomSheetDialogFragment?.show(
                supportFragmentManager,
                bottomSheetDialogFragment?.tag
            )
        }

    private fun updateOptionsMenu(state: FileInfoViewState) {
        if (state.jobInProgressState == null) {
            with(state) {
                menuHelper.setNodeName(title)
                menuHelper.updateOptionsMenu(
                    hasPreview = hasPreview,
                    isInInbox = isNodeInInbox,
                    isInRubbish = isNodeInRubbish,
                    fromIncomingShares = state.origin.fromInShares,
                    firstIncomingLevel = state.origin == FileInfoOrigin.IncomingSharesFirstLevel,
                    nodeAccess = accessPermission,
                    isTakenDown = isTakenDown,
                    isFile = isFile,
                    isExported = isExported
                )
            }
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
            is FileInfoOneOffViewEvent.Message -> showSnackBar(event.message)
            is FileInfoOneOffViewEvent.OverDiskQuota -> AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
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
                hideMultipleSelectOfSharedContacts()
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

    private fun updateCreationTime(viewState: FileInfoViewState) {
        bindingContent.filePropertiesInfoDataAdded.text = viewState.creationTimeText
        bindingContent.filePropertiesModifiedLayout.isVisible =
            viewState.modificationTimeText != null
        bindingContent.filePropertiesInfoDataModified.text = viewState.modificationTimeText
    }

    private fun updateIcon(viewState: FileInfoViewState) {
        viewState.iconResource?.let {
            binding.fileInfoToolbarIcon.setImageResource(it)
            binding.fileInfoToolbarIcon.isVisible = true
        } ?: run {
            binding.fileInfoToolbarIcon.isVisible = false
        }
    }

    private fun updateProperties(viewState: FileInfoViewState) {
        bindingContent.apply {
            Timber.d("refreshProperties")
            val isVisible = viewState.showLink
            dividerLinkLayout.isVisible = isVisible
            filePropertiesLinkLayout.isVisible = isVisible
            filePropertiesCopyLayout.isVisible = isVisible
            if (isVisible) {
                filePropertiesLinkText.text = viewState.publicLink
                filePropertiesLinkDate.text = getString(
                    R.string.general_date_label, TimeUtils.formatLongDateTime(
                        viewState.publicLinkCreationTime ?: 0
                    )
                )
            }
            updateSize(viewState.sizeInBytes)
            if (viewState.isFile) {
                filePropertiesInfoMenuSize.text =
                    getString(R.string.file_properties_info_size_file)
            }
        }
    }

    private fun updateTakenDown(isTakenDown: Boolean, isFile: Boolean) {
        bindingContent.warningBannerLayout.isVisible = isTakenDown
        if (isTakenDown) {
            bindingContent.takenDownFileWarningBanner.text =
                underlineText(
                    if (isFile) {
                        R.string.cloud_drive_info_taken_down_file_warning
                    } else {
                        R.string.cloud_drive_info_taken_down_folder_warning
                    }
                )
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

    private fun getNodeFromExtras() =
        intent.extras?.let { extras ->
            val handleNode = extras.getLong(Constants.HANDLE, -1)
            Timber.d("Handle of the selected node: %s", handleNode)
            megaApi.getNodeByHandle(handleNode)

        } ?: run {
            Timber.w("Extras is NULL")
            null
        }

    private fun getOriginFromExtras(): FileInfoOrigin {
        return when {
            intent.extras?.getInt(INTENT_EXTRA_KEY_FROM) == FROM_INCOMING_SHARES -> {
                if (intent.extras?.getBoolean(INTENT_EXTRA_KEY_FIRST_LEVEL) == true) {
                    FileInfoOrigin.IncomingSharesFirstLevel
                } else {
                    FileInfoOrigin.IncomingSharesOtherLevel
                }
            }
            intent.extras?.getInt(INTENT_EXTRA_KEY_ADAPTER_TYPE) == OUTGOING_SHARES_ADAPTER -> {
                FileInfoOrigin.OutgoingShares
            }
            else -> {
                FileInfoOrigin.Other
            }
        }
    }

    private fun setupView() {
        binding = ActivityFileInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            setSupportActionBar(toolbar)
            val rect = Rect()
            window.decorView.getWindowVisibleDisplayFrame(rect)
            menuHelper.setupToolbar(visibleTop = rect.top)
        }

        with(bindingContent) {
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

            //Content Layout
            filePropertiesLinkButton.text = getString(R.string.context_copy)

            val listView = fileInfoContactListView
            listView.itemAnimator = Util.noChangeRecyclerViewItemAnimator()
            listView.addItemDecoration(SimpleDividerItemDecoration(this@FileInfoActivity))
            val mLayoutManager = LinearLayoutManager(this@FileInfoActivity)
            listView.layoutManager = mLayoutManager

            //setup adapter
            listView.adapter = adapter
        }
    }

    private fun setupViewListeners() {
        with(bindingContent) {
            //Share with Layout
            filePropertiesSharedLayout.setOnClickListener {
                viewModel.expandOutSharesClick()
            }
            filePropertiesSharedInfoButton.setOnClickListener {
                viewModel.expandOutSharesClick()
            }
            filePropertiesSwitch.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
                viewModel.availableOfflineChanged(checked, WeakReference(this@FileInfoActivity))
            }

            filePropertiesLinkButton.setOnClickListener {
                Timber.d("Copy link button")
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Text", viewModel.uiState.value.publicLink)
                clipboard.setPrimaryClip(clip)
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.file_properties_get_link),
                    -1
                )
            }
            moreButton.setOnClickListener {
                val i = Intent(this@FileInfoActivity, FileContactListActivity::class.java)
                i.putExtra(Constants.NAME, viewModel.nodeId.longValue)
                startActivity(i)
            }
            filePropertiesInfoDataLocation.setOnClickListener {
                viewModel.uiState.value.nodeLocationInfo?.let { locationInfo ->
                    handleLocationClick(this@FileInfoActivity, adapterType, locationInfo)
                }
            }
            filePropertiesTextNumberVersions.setOnClickListener {
                versionHistoryLauncher.launch(viewModel.nodeId.longValue)
            }
            takenDownFileWarningBanner.setOnClickListener {
                redirectToTakedownPolicy()
            }
            takenDownFileWarningClose.setOnClickListener {
                bindingContent.warningBannerLayout.isVisible = false
            }
        }
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
                        shareFolder(NodeId(handle))
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
                    shareFolder(viewModel.nodeId)
                }
            }
            R.id.cab_menu_file_info_get_link, R.id.cab_menu_file_info_edit_link -> {
                if (showTakenDownNodeActionNotAvailableDialog(viewModel.node, this)) {
                    return false
                }
                LinksUtil.showGetLinkActivity(this, viewModel.nodeId.longValue)
            }
            R.id.cab_menu_file_info_remove_link -> {
                if (showTakenDownNodeActionNotAvailableDialog(viewModel.node, this)) {
                    return false
                }
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
                        viewModel.stopSharing()
                    }.setNegativeButton(getString(R.string.general_cancel), null)
                    .show()
            }
            R.id.cab_menu_file_info_copy -> copyLauncher.launch(longArrayOf(viewModel.nodeId.longValue))
            R.id.cab_menu_file_info_move -> moveLauncher.launch(longArrayOf(viewModel.nodeId.longValue))
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
                    if (!viewModel.uiState.value.isFile) {
                        if (fileBackupManager?.shareFolder(
                                nodeController,
                                longArrayOf(viewModel.nodeId.longValue),
                                contactsData,
                                MegaShare.ACCESS_READ
                            ) != true
                        ) {
                            showAccessPermissionOptions {
                                viewModel.setSharePermissionForUsers(
                                    it.toAccessPermission(),
                                    contactsData
                                )
                            }
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
                if (result == viewModel.nodeId.longValue) {
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
    private fun shareFolder(nodeId: NodeId) {
        selectContactForShareFolderLauncher.launch(nodeId)
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
                val handle = viewModel.nodeId.longValue
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
        nodeAttacher.saveState(outState)
        nodeSaver.saveState(outState)
    }

    /**
     * Receive the change permissions action to show the different permission options from bottom sheet
     */
    fun changePermissions() {
        Timber.d("changePermissions")
        showAccessPermissionOptions(viewModel.uiState.value.outShareContactShowOptions?.access) {
            viewModel.setSharePermissionForCurrentSelectedOptions(it.toAccessPermission())
        }
    }

    /**
     * Receive remove contact action to show a confirmation dialog
     */
    fun removeFileContactShare() = viewModel.uiState.value.outShareContactShowOptions?.let {
        showConfirmationRemoveContactsFromShare(it.user)
    }

    private fun hideMultipleSelectOfSharedContacts() {
        adapter.isMultipleSelect = false
        actionMode?.finish()
    }

    private fun showAccessPermissionOptions(
        selected: Int? = null,
        onSelect: (permission: Int) -> Unit,
    ) {
        val dialogBuilder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions))
        val items = arrayOf<CharSequence>(
            getString(R.string.file_properties_shared_folder_read_only),
            getString(R.string.file_properties_shared_folder_read_write),
            getString(R.string.file_properties_shared_folder_full_access)
        )
        dialogBuilder.setSingleChoiceItems(items, selected ?: -1) { dialog, item ->
            dialog.dismiss()
            onSelect(item)
        }.show()
    }

    private fun showConfirmationRemoveContactsFromShare(vararg contacts: String) =
        MaterialAlertDialogBuilder(this)
            .setMessage(
                contacts.singleOrNull()?.let { singleContact ->
                    getString(R.string.remove_contact_shared_folder, singleContact)
                } ?: resources.getQuantityString(
                    R.plurals.remove_multiple_contacts_shared_folder,
                    contacts.size,
                    contacts.size
                )
            )
            .setPositiveButton(getString(R.string.general_remove)) { _: DialogInterface?, _: Int ->
                viewModel.removeSharePermissionForUsers(*contacts)
            }
            .setNegativeButton(R.string.general_cancel, null)
            .show()

    // Clear all selected items
    private fun clearSelections() {
        if (adapter.isMultipleSelect) {
            adapter.clearSelections()
        }
        viewModel.contactsSelectedInSharedList(emptyList())
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
        viewModel.selectAllVisibleContacts()
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

        private fun Int.toAccessPermission() = when (this) {
            MegaShare.ACCESS_READ -> AccessPermission.READ
            MegaShare.ACCESS_READWRITE -> AccessPermission.READWRITE
            MegaShare.ACCESS_FULL -> AccessPermission.FULL
            MegaShare.ACCESS_OWNER -> AccessPermission.OWNER
            else -> AccessPermission.UNKNOWN
        }
    }
}