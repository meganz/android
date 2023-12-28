package mega.privacy.android.app.presentation.fileinfo

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.activities.contract.DeleteVersionsHistoryActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToCopyActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToMoveActivityContract
import mega.privacy.android.app.activities.contract.SelectUsersToShareActivityContract
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.interfaces.ActionBackupListener
import mega.privacy.android.app.main.FileContactListActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogListener
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoOneOffViewEvent
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.app.presentation.fileinfo.view.ExtraActionDialog
import mega.privacy.android.app.presentation.fileinfo.view.FileInfoScreen
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.presentation.transfers.startdownload.view.StartDownloadTransferComponent
import mega.privacy.android.app.sync.fileBackups.FileBackupManager
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.app.utils.MegaNodeDialogUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.handleLocationClick
import mega.privacy.android.app.utils.MegaNodeUtil.showConfirmationLeaveIncomingShare
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Activity for showing file and folder info.
 *
 * @property passCodeFacade [PasscodeCheck] an injected component to enforce a Passcode security check
 * @property getThemeMode [GetThemeMode] application them mode
 */
@AndroidEntryPoint
class FileInfoActivity : BaseActivity() {
    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private lateinit var selectContactForShareFolderLauncher: ActivityResultLauncher<NodeId>
    private lateinit var versionHistoryLauncher: ActivityResultLauncher<Long>
    private lateinit var copyLauncher: ActivityResultLauncher<LongArray>
    private lateinit var moveLauncher: ActivityResultLauncher<LongArray>

    private val viewModel: FileInfoViewModel by viewModels()
    private var adapterType = 0
    private var fileBackupManager: FileBackupManager? = null
    private val nodeController: NodeController by lazy { NodeController(this) }
    private val nodeAttacher by lazy { MegaAttacher(this) }
    private val nodeSaver by lazy {
        NodeSaver(
            this, this, this,
            AlertsAndWarnings.showSaveToDeviceConfirmDialog(this)
        )
    }

    private var bottomSheetDialogFragment: FileContactsListBottomSheetDialogFragment? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            retryConnectionsAndSignalPresence()
            if (viewModel.isAvailableOffline().not()) {
                val intent = Intent()
                intent.putExtra(NODE_HANDLE, viewModel.nodeId.longValue)
                setResult(RESULT_OK, intent)
            }
            finish()
        }
    }

    /**
     * on create the activity
     */
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewModel.setNode(readExtrasAndGetHandle() ?: run {
            finish()
            return
        })
        savedInstanceState?.apply {
            nodeAttacher.restoreState(this)
            nodeSaver.restoreState(this)
        }
        configureActivityResultLaunchers()
        initFileBackupManager()
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackBarHostState = remember { SnackbarHostState() }
            MegaAppTheme(isDark = themeMode.isDarkMode()) {
                EventEffect(
                    event = uiState.oneOffViewEvent,
                    onConsumed = viewModel::consumeOneOffEvent,
                    action = {
                        consumeEvent(it, snackBarHostState)
                    })
                FileInfoScreen(
                    viewState = uiState,
                    snackBarHostState = snackBarHostState,
                    onBackPressed = onBackPressedDispatcher::onBackPressed,
                    onTakeDownLinkClick = this::navigateToLink,
                    onLocationClick = { this.navigateToLocation(uiState.nodeLocationInfo) },
                    availableOfflineChanged = { availableOffline ->
                        viewModel.availableOfflineChanged(
                            availableOffline,
                            WeakReference(this@FileInfoActivity)
                        )
                    },
                    onVersionsClick = this::navigateToVersions,
                    onSharedWithContactClick = { this.navigateToUserDetails(it.contactItem) },
                    onSharedWithContactSelected = { viewModel.contactSelectedInSharedList(it.contactItem.email) },
                    onSharedWithContactUnselected = { viewModel.contactUnselectedInSharedList(it.contactItem.email) },
                    onSharedWithContactMoreOptionsClick = {
                        viewModel.contactToShowOptions(it.contactItem.email)
                    },
                    onShowMoreSharedWithContactsClick = this::navigateToSharedContacts,
                    onPublicLinkCopyClick = viewModel::copyPublicLink,
                    onMenuActionClick = { handleAction(it, uiState) },
                    onVerifyContactClick = this::navigateToVerifyContacts,
                    modifier = Modifier.semantics {
                        testTagsAsResourceId = true
                    }
                )
                uiState.requiredExtraAction?.let { action ->
                    ExtraActionDialog(
                        action = action,
                        onRemoveConfirmed = viewModel::removeConfirmed,
                        onPermissionSelected = viewModel::setSharePermissionForUsers,
                        onDismiss = viewModel::extraActionFinished,
                    )
                }
                StartDownloadTransferComponent(
                    uiState.downloadEvent,
                    { viewModel.consumeDownloadEvent() },
                    snackBarHostState = snackBarHostState,
                )
                updateContactShareBottomSheet(uiState)
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.setNode(
            handleNode = readExtrasAndGetHandle() ?: run { finish(); return },
            forceUpdate = true,
        )
    }

    private fun navigateToVerifyContacts(email: String) {
        val intent = Intent(this, AuthenticityCredentialsActivity::class.java)
        intent.putExtra(Constants.EMAIL, email)
        startActivity(intent)
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
        if (nodeAttacher.handleActivityResult(requestCode, resultCode, data, this)) {
            return
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
    }


    /**
     * Called to retrieve per-instance state from an activity before being killed so that the state can be restored
     */
    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        nodeAttacher.saveState(outState)
        nodeSaver.saveState(outState)
    }

    private fun readExtrasAndGetHandle() = intent.extras?.let { extras ->
        val handleNode = extras.getLong(Constants.HANDLE, -1)
        Timber.d("Handle of the selected node: %s", handleNode)
        adapterType = intent.getIntExtra(
            Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
            Constants.FILE_BROWSER_ADAPTER
        )
        handleNode.takeIf { it >= 0 }
    } ?: run {
        Timber.w("Extras is NULL")
        null
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
                    if (actionType == ACTION_BACKUP_SHARE_FOLDER && operationType == FileBackupManager.OperationType.OPERATION_EXECUTE) {
                        navigateToShare(NodeId(handle))
                    }
                }
            })
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
                    if (!viewModel.isFile()) {
                        if (fileBackupManager?.shareFolder(
                                nodeController,
                                longArrayOf(viewModel.nodeId.longValue),
                                contactsData,
                                MegaShare.ACCESS_READ
                            ) != true
                        ) {
                            viewModel.initiateChangePermission(contactsData)
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

    private fun handleAction(action: FileInfoMenuAction, viewState: FileInfoViewState) {
        when (action) {
            FileInfoMenuAction.Copy -> navigateToCopy()
            FileInfoMenuAction.Move -> navigateToMove()
            FileInfoMenuAction.Delete -> viewModel.initiateRemoveNode(sendToRubbish = false)
            FileInfoMenuAction.MoveToRubbishBin -> viewModel.initiateRemoveNode(sendToRubbish = true)
            FileInfoMenuAction.Rename -> showRenameDialog()
            FileInfoMenuAction.Download -> downloadNode()
            FileInfoMenuAction.GetLink, FileInfoMenuAction.ManageLink -> navigateToGetLink()
            FileInfoMenuAction.RemoveLink -> viewModel.initiateRemoveLink()
            FileInfoMenuAction.ShareFolder -> showShareFolderDialog()
            FileInfoMenuAction.Leave -> showConfirmLeaveDialog()
            FileInfoMenuAction.SendToChat -> navigateToSendToChat()
            FileInfoMenuAction.DisputeTakedown -> navigateToDisputeTakeDown()
            FileInfoMenuAction.SelectionModeAction.ChangePermission -> {
                viewModel.initiateChangePermission(null)
            }

            FileInfoMenuAction.SelectionModeAction.ClearSelection -> viewModel.unselectAllContacts()
            FileInfoMenuAction.SelectionModeAction.Remove -> {
                viewModel.initiateRemoveContacts(viewState.outShareContactsSelected)
            }

            FileInfoMenuAction.SelectionModeAction.SelectAll -> viewModel.selectAllVisibleContacts()
        }
    }

    private fun navigateToLink(link: String) {
        val uriUrl = Uri.parse(link)
        val launchBrowser = Intent(this, WebViewActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setData(uriUrl)
        startActivity(launchBrowser)
    }

    private fun navigateToLocation(locationInfo: LocationInfo?) {
        locationInfo?.let {
            handleLocationClick(this, adapterType, locationInfo)
        }
    }

    private fun navigateToVersions() {
        versionHistoryLauncher.launch(viewModel.nodeId.longValue)
    }

    private fun navigateToShare(nodeId: NodeId = viewModel.nodeId) {
        selectContactForShareFolderLauncher.launch(nodeId)
    }

    private fun navigateToSharedContacts() {
        startActivity(
            Intent(this, FileContactListActivity::class.java).apply {
                putExtra(Constants.NAME, viewModel.nodeId.longValue)
            }
        )
    }

    private fun navigateToUserDetails(contactItem: ContactItem) {
        ContactUtil.openContactInfoActivity(this, contactItem.email)
    }

    private fun navigateToCopy() = copyLauncher.launch(longArrayOf(viewModel.nodeId.longValue))
    private fun navigateToMove() = moveLauncher.launch(longArrayOf(viewModel.nodeId.longValue))

    private fun navigateToSendToChat() {
        Timber.d("Send chat option")
        nodeAttacher.attachNode(viewModel.node)
    }

    private fun navigateToDisputeTakeDown() =
        startActivity(
            Intent(this, WebViewActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setData(Uri.parse(Constants.DISPUTE_URL))
        )

    private fun navigateToGetLink() {
        if (showTakenDownNodeActionNotAvailableDialog(viewModel.node, this)) {
            return
        }
        LinksUtil.showGetLinkActivity(this, viewModel.nodeId.longValue)
    }

    private fun downloadNode() {
        PermissionUtils.checkNotificationsPermission(this)
        viewModel.startDownloadNode()
    }

    private fun showConfirmLeaveDialog() {
        showConfirmationLeaveIncomingShare(
            this,
            this,
            viewModel.node
        )
    }

    private fun showShareFolderDialog() {
        val nodeType = MegaNodeUtil.checkBackupNodeTypeByHandle(megaApi, viewModel.node)
        if (nodeType != MegaNodeDialogUtil.BACKUP_NONE) {
            // Display a warning dialog when sharing a Backup folder and limit folder
            // access to read-only
            fileBackupManager?.defaultActionBackupNodeCallback?.let {
                fileBackupManager?.shareBackupsFolder(
                    nodeController = nodeController,
                    megaNode = viewModel.node,
                    nodeType = nodeType,
                    actionBackupNodeCallback = it,
                )
            }
        } else {
            navigateToShare()
        }
    }

    private fun showRenameDialog() =
        showRenameNodeDialog(this, viewModel.node, this, null)

    private suspend fun consumeEvent(
        event: FileInfoOneOffViewEvent,
        snackBarHostState: SnackbarHostState,
    ) {
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

            FileInfoOneOffViewEvent.PublicLinkCopiedToClipboard -> {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    //Android 13 and up shows a system notification, so no need to show the toast
                    snackBarHostState.showSnackbar(getString(R.string.file_properties_get_link))
                }
            }

            is FileInfoOneOffViewEvent.GeneralError -> {
                snackBarHostState.showSnackbar(getString(R.string.general_error))
            }

            is FileInfoOneOffViewEvent.CollisionDetected -> {
                val list = ArrayList<NameCollision>()
                list.add(event.collision)
                nameCollisionActivityContract?.launch(list)
            }

            is FileInfoOneOffViewEvent.Finished -> {
                if (event.exception == null) {
                    event.successMessage(this)?.let {
                        snackBarHostState.showSnackbar(it)
                    }
                    sendBroadcast(Intent(Constants.BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN).setPackage(applicationContext.packageName))
                } else {
                    Timber.e(event.exception)
                    if (!manageCopyMoveException(event.exception)) {
                        event.failMessage(this)?.let {
                            snackBarHostState.showSnackbar(it)
                        }
                    }
                }
            }

            is FileInfoOneOffViewEvent.Message -> {
                snackBarHostState.showSnackbar(getString(event.message))
            }

            is FileInfoOneOffViewEvent.OverDiskQuota -> AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
            FileInfoOneOffViewEvent.StartLegacyDownload -> {
                nodeSaver.saveNode(
                    viewModel.node,
                    highPriority = false,
                    isFolderLink = false,
                    fromMediaViewer = false,
                    needSerialize = false
                )
            }
        }
    }

    private fun updateContactShareBottomSheet(viewState: FileInfoViewState) =
        viewState.contactToShowOptions?.takeIf {
            !bottomSheetDialogFragment.isBottomSheetDialogShown() //this is not compose yet, so we need to check if it's already shown
        }?.let { email ->
            Timber.d("showNodeOptionsPanel")
            bottomSheetDialogFragment =
                FileContactsListBottomSheetDialogFragment(
                    viewModel.getShareFromEmail(email),
                    viewModel.node,
                    object : FileContactsListBottomSheetDialogListener {
                        override fun changePermissions(userEmail: String) {
                            viewModel.initiateChangePermission(listOf(userEmail))
                        }

                        override fun removeFileContactShare(userEmail: String) {
                            viewModel.initiateRemoveContacts(listOf(userEmail))
                        }

                        override fun fileContactsDialogDismissed() {
                            viewModel.contactToShowOptions(null)
                        }

                    }
                )
            bottomSheetDialogFragment?.show(
                supportFragmentManager,
                bottomSheetDialogFragment?.tag
            )
        }

    companion object {
        /**
         * key to return the handle to the calling activity
         */
        const val NODE_HANDLE = "NODE_HANDLE"
    }
}