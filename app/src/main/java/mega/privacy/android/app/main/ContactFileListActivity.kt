package mega.privacy.android.app.main

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToCopyActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.extensions.consumeInsetsWithToolbar
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.modalbottomsheet.ContactFileListBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.OnSharedFolderUpdatedCallBack
import mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment
import mega.privacy.android.app.presentation.bottomsheet.UploadBottomSheetDialogActionListener
import mega.privacy.android.app.presentation.contact.ContactFileListViewModel
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsActivity
import mega.privacy.android.app.presentation.documentscanner.dialogs.DocumentScanningErrorDialog
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.extensions.uploadFolderManually
import mega.privacy.android.core.nodecomponents.mapper.message.NodeMoveRequestMessageMapper
import mega.privacy.android.app.presentation.node.dialogs.leaveshare.LeaveShareDialog
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil.IS_NEW_FOLDER_DIALOG_SHOWN
import mega.privacy.android.app.utils.MegaNodeDialogUtil.IS_NEW_TEXT_FILE_SHOWN
import mega.privacy.android.app.utils.MegaNodeDialogUtil.NEW_FOLDER_DIALOG_TEXT
import mega.privacy.android.app.utils.MegaNodeDialogUtil.NEW_TEXT_FILE_TEXT
import mega.privacy.android.app.utils.MegaNodeDialogUtil.checkNewFolderDialogState
import mega.privacy.android.app.utils.MegaNodeDialogUtil.checkNewTextFileDialogState
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showNewFolderDialog
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showNewTxtFileDialog
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.MegaProgressDialogUtil.showProcessFileDialog
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.UploadUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.getAudioPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getImagePermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getReadExternalStoragePermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getVideoPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.file.CheckFileNameCollisionsUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.DocumentScanInitiatedEvent
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
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
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class ContactFileListActivity : PasscodeActivity(), MegaGlobalListenerInterface,
    MegaRequestListenerInterface, UploadBottomSheetDialogActionListener, ActionNodeCallback,
    OnSharedFolderUpdatedCallBack, SnackbarShower {

    @Inject
    lateinit var getNodeByHandleUseCase: GetNodeByHandleUseCase

    @Inject
    lateinit var checkFileNameCollisionsUseCase: CheckFileNameCollisionsUseCase

    @Inject
    lateinit var copyRequestMessageMapper: CopyRequestMessageMapper

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    /**
     * The Application Theme Mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var nodeMoveRequestMessageMapper: NodeMoveRequestMessageMapper
    private val viewModel: ContactFileListViewModel by viewModels()
    private val startDownloadViewModel: StartDownloadViewModel by viewModels()
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var documentScanningErrorDialogComposeView: ComposeView
    private var userEmail: String? = null
    private var contact: MegaUser? = null
    private var fullName = ""
    private var contactFileListFragment: ContactFileListFragment? = null
    private var parentHandle: Long = -1
    private var newFolderDialog: AlertDialog? = null
    private var statusDialog: AlertDialog? = null
    var selectedNode: MegaNode? = null
    private var tB: Toolbar? = null
    private var aB: ActionBar? = null
    private var bottomSheetDialogFragment: BottomSheetDialogFragment? = null
    private var newTextFileDialog: AlertDialog? = null

    override fun onSharedFolderUpdated() {
        Timber.d("onSharedFolderUpdated")
        contactFileListFragment?.takeIf { it.isVisible }?.let {
            it.clearSelections()
            it.hideMultipleSelect()
        }
        statusDialog?.dismiss()
    }

    override fun showLeaveFolderDialog(nodeIds: List<Long>) {
        viewModel.setLeaveFolderNodeIds(nodeIds)
    }

    private val selectFolderToCopyLauncher: ActivityResultLauncher<LongArray> =
        registerForActivityResult(SelectFolderToCopyActivityContract()) { result ->
            val copyHandles = result?.first?.toList() ?: return@registerForActivityResult
            viewModel.copyOrMoveNodes(
                nodes = copyHandles,
                targetNode = result.second,
                type = NodeNameCollisionType.COPY
            )
        }

    private val nameCollisionActivityLauncher = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        result?.let {
            showSnackbar(SNACKBAR_TYPE, it, INVALID_HANDLE)
        }
    }

    /**
     * Launcher to scan documents using the ML Kit Document Scanner. After scanning, a different
     * screen is opened to configure where to save the scanned documents.
     */
    private val scanDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.let { data ->
                    with(data) {
                        val imageUris = pages?.mapNotNull { page ->
                            page.imageUri
                        } ?: emptyList()

                        // The PDF URI must exist before moving to the Scan Confirmation page
                        pdf?.uri?.let { pdfUri ->
                            val intent = Intent(
                                this@ContactFileListActivity,
                                SaveScannedDocumentsActivity::class.java,
                            ).apply {
                                putExtra(
                                    SaveScannedDocumentsActivity.EXTRA_ORIGINATED_FROM_CHAT,
                                    false,
                                )
                                putExtra(
                                    SaveScannedDocumentsActivity.EXTRA_CLOUD_DRIVE_PARENT_HANDLE,
                                    getParentHandle(),
                                )
                                putExtra(SaveScannedDocumentsActivity.EXTRA_SCAN_PDF_URI, pdfUri)
                                putExtra(
                                    SaveScannedDocumentsActivity.EXTRA_SCAN_SOLO_IMAGE_URI,
                                    if (imageUris.size == 1) imageUris[0] else null,
                                )
                            }
                            this@ContactFileListActivity.startActivity(intent)
                        } ?: run {
                            Timber.e("The PDF file could not be retrieved from Contact File List after scanning")
                        }
                    }
                }
            } else {
                Timber.e("The ML Kit Document Scan result could not be retrieved from Contact File List")
            }
        }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(PARENT_HANDLE, parentHandle)
        checkNewTextFileDialogState(newTextFileDialog, outState)
        newFolderDialog.checkNewFolderDialogState(outState)
        super.onSaveInstanceState(outState)
    }

    /**
     * When manually uploading Files and the device is running Android 13 and above, this Launcher
     * is called to request the Notification Permission (if possible) and upload Files regardless
     * if the Notification Permission is granted or not
     */
    private val manualUploadFilesLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _: Boolean? -> uploadFilesManually() }

    /**
     * Launch the system file picker to select multiple files
     */
    private val openMultipleDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            if (it.isNotEmpty()) {
                handleFileUris(it)
            }
        }

    /**
     * When manually uploading a Folder and the device is running Android 13 and above, this Launcher
     * is called to request the Notification Permission whenever possible, and upload the Folder
     * regardless if the Notification Permission is granted or not
     */
    private val manualUploadFolderLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean? -> this.uploadFolderManually() }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        } else if (item.itemId == R.id.action_more) {
            showOptionsPanel(megaApi.getNodeByHandle(parentHandle))
        }
        return true
    }

    override fun uploadFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manualUploadFilesLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            uploadFilesManually()
        }
    }

    override fun uploadFolder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manualUploadFolderLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            this.uploadFolderManually()
        }
    }

    override fun takePictureAndUpload() {
        if (!hasPermissions(this, Manifest.permission.CAMERA)) {
            requestPermission(this, Constants.REQUEST_CAMERA, Manifest.permission.CAMERA)
            return
        }
        if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermission(
                this,
                Constants.REQUEST_WRITE_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            return
        }
        Util.checkTakePicture(this, Constants.TAKE_PHOTO_CODE)
    }

    override fun scanDocument() {
        viewModel.prepareDocumentScanner()
    }

    override fun showNewFolderDialog(typedText: String?) {
        newFolderDialog =
            showNewFolderDialog(this, this, megaApi.getNodeByHandle(parentHandle), typedText)
    }

    override fun showNewTextFileDialog(typedName: String?) {
        megaApi.getNodeByHandle(parentHandle)?.let {
            newTextFileDialog = showNewTxtFileDialog(this, it, typedName, false)
        }
    }

    override fun createFolder(folderName: String) {
        Timber.d("createFolder")
        if (!viewModel.isOnline()) {
            showSnackbar(
                SNACKBAR_TYPE,
                getString(R.string.error_server_connection_problem)
            )
            return
        }
        if (isFinishing) {
            return
        }
        val parentHandle = contactFileListFragment?.parentHandle ?: return
        val parentNode = megaApi.getNodeByHandle(parentHandle) ?: return
        Timber.d("parentNode != null: %s", parentNode.name)
        val nL = megaApi.getChildren(parentNode).orEmpty()
        val exists = nL.any { it.name == folderName }
        if (!exists) {
            statusDialog = null
            try {
                statusDialog = createProgressDialog(
                    this,
                    getString(R.string.context_creating_folder)
                ).also {
                    it.show()
                }
            } catch (e: Exception) {
                return
            }
            megaApi.createFolder(folderName, parentNode, this)
        } else {
            showSnackbar(
                SNACKBAR_TYPE,
                getString(R.string.context_folder_already_exists)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return
        }
        if (savedInstanceState == null) {
            setParentHandle(-1)
        } else {
            setParentHandle(savedInstanceState.getLong(PARENT_HANDLE, -1))
        }
        megaApi.addGlobalListener(this)
        val extras = intent.extras
        if (extras != null) {
            userEmail = extras.getString(Constants.NAME)
            val currNodePosition = extras.getInt("node_position", -1)
            enableEdgeToEdge()
            setContentView(R.layout.activity_main_contact_properties)
            consumeInsetsWithToolbar(customToolbar = findViewById(R.id.app_bar_layout))

            //Set toolbar
            tB =
                findViewById<View>(R.id.toolbar_main_contact_properties) as Toolbar
            if (tB == null) {
                Timber.w("Toolbar is NULL")
            }
            setSupportActionBar(tB)
            aB = supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
                setTitleActionBar(null)
            }
            contact = megaApi.getContact(userEmail)
            if (contact == null) {
                finish()
            }
            fullName = ContactUtil.getMegaUserNameDB(contact)
            fragmentContainer =
                findViewById<View>(R.id.fragment_container_contact_properties) as FrameLayout
            Timber.d("Shared Folders are:")
            contactFileListFragment =
                supportFragmentManager.findFragmentByTag("cflF") as ContactFileListFragment?
            if (contactFileListFragment == null) {
                contactFileListFragment = ContactFileListFragment()
            }
            contactFileListFragment?.let {
                it.setUserEmail(userEmail)
                it.currNodePosition = currNodePosition
                it.parentHandle = parentHandle
                supportFragmentManager.beginTransaction().replace(
                    R.id.fragment_container_contact_properties,
                    it,
                    "cflF"
                ).commitNow()
            }
            if (savedInstanceState != null && savedInstanceState.getBoolean(
                    IS_NEW_TEXT_FILE_SHOWN,
                    false
                )
            ) {
                showNewTextFileDialog(savedInstanceState.getString(NEW_TEXT_FILE_TEXT))
            }
            if (savedInstanceState != null && savedInstanceState.getBoolean(
                    IS_NEW_FOLDER_DIALOG_SHOWN,
                    false
                )
            ) {
                showNewFolderDialog(savedInstanceState.getString(NEW_FOLDER_DIALOG_TEXT))
            }
            documentScanningErrorDialogComposeView =
                findViewById(R.id.contact_properties_error_dialog_compose_view)
            setComposeProperties()
            setUpLeaveSharesDialog()
        }
        collectFlows()
    }

    private fun setUpLeaveSharesDialog() {
        findViewById<ComposeView>(R.id.leave_shares_dialog)?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                OriginalTheme(isDark = isDark) {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    state.leaveFolderNodeIds?.let {
                        LeaveShareDialog(handles = it, onDismiss = {
                            onSharedFolderUpdated()
                            viewModel.clearLeaveFolderNodeIds()
                        })
                    }
                }
            }
        }
    }

    /**
     * Sets up the Compose Views for this Activity using the traditional View system
     */
    private fun setComposeProperties() {
        documentScanningErrorDialogComposeView.apply {
            setContent {
                val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                val state by viewModel.state.collectAsStateWithLifecycle()

                OriginalTheme(isDark = isDark) {
                    DocumentScanningErrorDialog(
                        documentScanningError = state.documentScanningError,
                        onErrorAcknowledged = { viewModel.onDocumentScanningErrorConsumed() },
                        onErrorDismissed = { viewModel.onDocumentScanningErrorConsumed() },
                    )
                }
            }
        }
    }

    private fun collectFlows() {
        collectFlow(viewModel.state) { uiState ->
            val nodeNameCollisionResult = uiState.nodeNameCollisionResult
            if (nodeNameCollisionResult.isNotEmpty()) {
                handleNodesNameCollisionResult(nodeNameCollisionResult)
                viewModel.markHandleNodeNameCollisionResult()
            }
            if (uiState.moveRequestResult != null) {
                handleMovementResult(uiState.moveRequestResult)
                viewModel.markHandleMoveRequestResult()
            }
            if (uiState.copyMoveAlertTextId != null) {
                if (statusDialog == null) {
                    statusDialog =
                        createProgressDialog(this, getString(uiState.copyMoveAlertTextId))
                }
                statusDialog?.show()
            } else {
                dismissAlertDialogIfExists(statusDialog)
            }
            uiState.snackBarMessage?.let {
                showSnackbar(
                    SNACKBAR_TYPE,
                    getString(it),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
                viewModel.onConsumeSnackBarMessageEvent()
            }

            if (uiState.uploadEvent is StateEventWithContentTriggered) {
                startDownloadViewModel.onUploadClicked(uiState.uploadEvent.content)
            }

            uiState.gmsDocumentScanner?.let { gmsDocumentScanner ->
                scanDocument(gmsDocumentScanner)
                viewModel.onGmsDocumentScannerConsumed()
            }
        }
    }

    /**
     * Begin scanning Documents using the ML Kit Document Scanner
     *
     * @param documentScanner the ML Kit Document Scanner
     */
    private fun scanDocument(documentScanner: GmsDocumentScanner) {
        documentScanner.apply {
            getStartScanIntent(this@ContactFileListActivity)
                .addOnSuccessListener {
                    Analytics.tracker.trackEvent(DocumentScanInitiatedEvent)
                    scanDocumentLauncher.launch(IntentSenderRequest.Builder(it).build())
                }
                .addOnFailureListener { exception ->
                    Timber.e(
                        exception,
                        "An error occurred when attempting to run the ML Kit Document Scanner from Contact File List",
                    )
                    viewModel.onDocumentScannerFailedToOpen()
                }
        }
    }

    private fun handleMovementResult(moveRequestResult: Result<MoveRequestResult>) {
        dismissAlertDialogIfExists(statusDialog)
        actionConfirmed()
        if (moveRequestResult.isSuccess) {
            val data = moveRequestResult.getOrThrow()
            if (data !is MoveRequestResult.DeleteMovement && data.nodes.isNotEmpty()) {
                showMovementResult(data, data.nodes.first())
            }
            showSnackbar(
                SNACKBAR_TYPE,
                nodeMoveRequestMessageMapper(data),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
        } else {
            manageCopyMoveException(moveRequestResult.exceptionOrNull())
        }
    }

    private fun handleNodesNameCollisionResult(conflictNodes: List<NameCollision>) {
        if (conflictNodes.isNotEmpty()) {
            dismissAlertDialogIfExists(statusDialog)
            nameCollisionActivityLauncher.launch(ArrayList(conflictNodes))
        }
    }

    fun showUploadPanel() {
        Timber.d("showUploadPanel")
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            getImagePermissionByVersion(),
            getAudioPermissionByVersion(),
            getVideoPermissionByVersion(),
            getReadExternalStoragePermission()
        )
        if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermission(this, Constants.REQUEST_READ_WRITE_STORAGE, *permissions)
        } else {
            onGetReadWritePermission()
        }
    }

    private fun onGetReadWritePermission() {
        if (bottomSheetDialogFragment.isBottomSheetDialogShown()) return
        val bottomSheetDialogFragment = UploadBottomSheetDialogFragment()
        bottomSheetDialogFragment.show(supportFragmentManager, bottomSheetDialogFragment.tag)
    }

    override fun onResume() {
        Timber.d("onResume")
        super.onResume()
        val intent = intent
        if (intent != null) {
            intent.action = null
            setIntent(null)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        Timber.d("onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            return
        }
        when (requestCode) {
            Constants.REQUEST_CAMERA -> {
                if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    requestPermission(
                        this,
                        Constants.REQUEST_WRITE_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                } else {
                    takePictureAndUpload()
                }
            }

            Constants.REQUEST_READ_WRITE_STORAGE -> {
                Timber.d("REQUEST_READ_WRITE_STORAGE")
                onGetReadWritePermission()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        Timber.d("onNewIntent")
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        Timber.d("onDestroy()")
        super.onDestroy()
        megaApi.removeGlobalListener(this)
        megaApi.removeRequestListener(this)
        dismissAlertDialogIfExists(newFolderDialog)
    }

    fun setParentHandle(parentHandle: Long) {
        this.parentHandle = parentHandle
    }

    fun downloadFile(nodes: List<MegaNode>) {
        startDownloadViewModel.onDownloadClicked(
            nodeIds = nodes.map { NodeId(it.handle) },
            isHighPriority = true,
            withStartMessage = true,
        )
    }

    private fun moveToTrash(handleList: ArrayList<Long>) {
        Timber.d("moveToTrash: ")
        if (!viewModel.isOnline()) {
            showSnackbar(
                SNACKBAR_TYPE,
                getString(R.string.error_server_connection_problem)
            )
            return
        }
        viewModel.moveNodesToRubbish(handleList)
    }

    /**
     * Shows the final result of a movement request.
     *
     * @param result Object containing the request result.
     * @param handle Handle of the node to move.
     */
    private fun showMovementResult(result: MoveRequestResult, handle: Long) {
        if (result.isSingleAction && result.isSuccess && parentHandle == handle) {
            onBackPressed()
            setTitleActionBar(megaApi.getNodeByHandle(parentHandle)!!.name)
        }
    }

    fun showMove(handleList: ArrayList<Long>) {
        val intent = Intent(this, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_MOVE_FOLDER
        intent.putExtra("MOVE_FROM", handleList.toLongArray())
        startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE)
    }

    fun showCopy(handleList: ArrayList<Long>) {
        selectFolderToCopyLauncher.launch(handleList.toLongArray())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE && resultCode == RESULT_OK) {
            if (intent == null) {
                return
            }
            val moveHandles = intent.getLongArrayExtra("MOVE_HANDLES")
            val toHandle = intent.getLongExtra("MOVE_TO", 0)

            if (moveHandles == null || moveHandles.isEmpty()) return
            viewModel.copyOrMoveNodes(
                nodes = moveHandles.toList(),
                targetNode = toHandle,
                type = NodeNameCollisionType.MOVE
            )
        } else if (requestCode == Constants.REQUEST_CODE_GET_FOLDER) {
            UploadUtil.getFolder(this, resultCode, intent, parentHandle)
        } else if (requestCode == Constants.REQUEST_CODE_GET_FOLDER_CONTENT) {
            if (intent != null && resultCode == RESULT_OK) {
                val result = intent.getStringExtra(Constants.EXTRA_ACTION_RESULT)
                if (TextUtil.isTextEmpty(result)) {
                    return
                }
                showSnackbar(SNACKBAR_TYPE, result)
            }
        } else if (requestCode == Constants.TAKE_PHOTO_CODE) {
            if (resultCode == RESULT_OK) {
                val parentHandle = contactFileListFragment?.parentHandle ?: return
                val file = UploadUtil.getTemporalTakePictureFile(this)
                if (file != null) {
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
                        }.onSuccess { fileCollisions ->
                            val collision = fileCollisions.firstOrNull()
                            if (collision != null) {
                                nameCollisionActivityLauncher.launch(arrayListOf(collision))
                            } else {
                                viewModel.uploadFile(
                                    file = file,
                                    destination = parentHandle
                                )
                            }
                        }.onFailure {
                            Timber.e(it, "Cannot check name collisions")
                            showSnackbar(
                                SNACKBAR_TYPE,
                                getString(R.string.general_error)
                            )
                        }
                    }
                }
            } else {
                Timber.w("TAKE_PHOTO_CODE--->ERROR!")
            }
        }
    }

    /**
     * Handle processed upload intent.
     *
     * @param infos list of DocumentEntity
     */
    private fun onIntentProcessed(infos: List<DocumentEntity>) {
        val parentNode = megaApi.getNodeByHandle(parentHandle)
        if (parentNode == null) {
            dismissAlertDialogIfExists(statusDialog)
            Util.showErrorAlertDialog(
                getString(R.string.error_temporary_unavaible),
                false,
                this
            )
            return
        }
        if (infos.isEmpty()) {
            dismissAlertDialogIfExists(statusDialog)
            Util.showErrorAlertDialog(
                getString(R.string.upload_can_not_open),
                false, this
            )
            return
        }
        if (viewModel.getStorageState() === StorageState.PayWall) {
            dismissAlertDialogIfExists(statusDialog)
            showOverDiskQuotaPaywallWarning()
            return
        }

        lifecycleScope.launch {
            runCatching {
                checkFileNameCollisionsUseCase(
                    files = infos,
                    parentNodeId = NodeId(parentNode.handle)
                )
            }.onSuccess { collisions ->
                dismissAlertDialogIfExists(statusDialog)
                if (collisions.isNotEmpty()) {
                    nameCollisionActivityLauncher.launch(collisions.toCollection(ArrayList()))
                }
                val collidedSharesPath = collisions.map { it.path.value }.toSet()
                val sharesWithoutCollision = infos.filter {
                    collidedSharesPath.contains(it.uri.value).not()
                }
                if (sharesWithoutCollision.isNotEmpty()) {
                    viewModel.uploadFiles(
                        pathsAndNames = sharesWithoutCollision.map { it.uri.value }
                            .associateWith { null },
                        destinationId = NodeId(parentNode.handle)
                    )
                }
            }.onFailure {
                dismissAlertDialogIfExists(statusDialog)
                Util.showErrorAlertDialog(
                    getString(R.string.error_temporary_unavaible),
                    false,
                    this@ContactFileListActivity
                )
            }
        }
    }

    override fun onBackPressed() {
        retryConnectionsAndSignalPresence()
        if (contactFileListFragment?.onBackPressed() == 0) {
            super.onBackPressed()
        }
    }

    override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser>?) {}
    override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert>?) {
        Timber.d("onUserAlertsUpdate")
    }

    override fun onNodesUpdate(api: MegaApiJava, nodes: ArrayList<MegaNode>?) {
        for (node in nodes.orEmpty()) {
            if (node.isInShare && parentHandle == node.handle) {
                lifecycleScope.launch {
                    runCatching {
                        getNodeByHandleUseCase(
                            handle = node.handle,
                            attemptFromFolderApi = true
                        )
                    }.onSuccess {
                        updateNodes()
                    }.onFailure {
                        finish()
                    }
                }
            } else {
                updateNodes()
            }
        }
    }

    /**
     * Update the nodes.
     */
    private fun updateNodes() {
        contactFileListFragment?.takeIf { it.isVisible }?.setNodes(parentHandle)
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        if (request.type == MegaRequest.TYPE_MOVE) {
            Timber.d("Move request start")
        } else if (request.type == MegaRequest.TYPE_REMOVE) {
            Timber.d("Remove request start")
        } else if (request.type == MegaRequest.TYPE_EXPORT) {
            Timber.d("Export request start")
        } else if (request.type == MegaRequest.TYPE_SHARE) {
            Timber.d("Share request start")
        }
    }

    fun askConfirmationMoveToRubbish(handleList: ArrayList<Long>) {
        Timber.d("askConfirmationMoveToRubbish")
        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> moveToTrash(handleList)
                DialogInterface.BUTTON_NEGATIVE -> {}
            }
        }
        if (handleList.size > 0) {
            val builder = MaterialAlertDialogBuilder(
                this,
                R.style.ThemeOverlay_Mega_MaterialAlertDialog
            )
            if (handleList.size > 1) {
                builder.setMessage(resources.getString(R.string.confirmation_move_to_rubbish_plural))
            } else {
                builder.setMessage(resources.getString(R.string.confirmation_move_to_rubbish))
            }
            builder.setPositiveButton(
                R.string.general_move,
                dialogClickListener
            )
            builder.setNegativeButton(
                sharedR.string.general_dialog_cancel_button,
                dialogClickListener
            )
            builder.show()
        }
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestUpdate")
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish")
        if (request.type == MegaRequest.TYPE_CREATE_FOLDER) {
            try {
                statusDialog?.dismiss()
            } catch (ex: Exception) {
                Timber.e(ex)
            }
            if (e.errorCode == MegaError.API_OK) {
                val folderNode = megaApi.getNodeByHandle(request.nodeHandle) ?: return
                if (contactFileListFragment?.isVisible == true) {
                    showSnackbar(
                        SNACKBAR_TYPE,
                        getString(R.string.context_folder_created)
                    )
                    contactFileListFragment?.navigateToFolder(folderNode)
                }
            } else {
                if (contactFileListFragment?.isVisible == true) {
                    showSnackbar(
                        SNACKBAR_TYPE,
                        getString(R.string.context_folder_no_created)
                    )
                    contactFileListFragment?.setNodes()
                }
            }
        }
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava, request: MegaRequest,
        e: MegaError,
    ) {
        Timber.d("onRequestTemporaryError")
    }

    override fun onAccountUpdate(api: MegaApiJava) {}
    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest>?,
    ) {
    }

    override fun onEvent(api: MegaApiJava, event: MegaEvent?) {}
    override fun onSetsUpdate(api: MegaApiJava, sets: ArrayList<MegaSet>?) {}
    override fun onSetElementsUpdate(api: MegaApiJava, elements: ArrayList<MegaSetElement>?) {}
    override fun onGlobalSyncStateChanged(api: MegaApiJava) {}

    fun showOptionsPanel(node: MegaNode?) {
        Timber.d("showOptionsPanel")
        if (node == null || bottomSheetDialogFragment.isBottomSheetDialogShown()) return
        selectedNode = node
        bottomSheetDialogFragment = ContactFileListBottomSheetDialogFragment().also {
            it.show(supportFragmentManager, "ContactFileListBottomSheetDialogFragment")
        }
    }

    fun showSnackbar(type: Int, s: String?) {
        val coordinatorFragment =
            findViewById<View>(R.id.contact_file_list_coordinator_layout) as? CoordinatorLayout
        contactFileListFragment =
            supportFragmentManager.findFragmentByTag("cflF") as ContactFileListFragment?
        if (contactFileListFragment != null && contactFileListFragment!!.isVisible) {
            if (coordinatorFragment != null) {
                showSnackbar(type, coordinatorFragment, s)
            } else {
                showSnackbar(type, fragmentContainer, s)
            }
        }
    }

    val isEmptyParentHandleStack: Boolean
        get() = contactFileListFragment?.isEmptyParentHandleStack ?: true

    fun setTitleActionBar(title: String?) {
        aB?.apply {
            if (title == null) {
                Timber.d("Reset title and subtitle")
                this.title =
                    getString(R.string.title_incoming_shares_with_explorer)
                subtitle = fullName
            } else {
                this.title = title
                subtitle = null
            }
        }
    }

    fun getParentHandle(): Long = contactFileListFragment?._parentHandle ?: -1

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, fragmentContainer, content, chatId)
    }

    override fun finishRenameActionWithSuccess(newName: String) {
        // No update needed
    }

    override fun actionConfirmed() {
        contactFileListFragment?.takeIf { it.isVisible }?.apply {
            clearSelections()
            hideMultipleSelect()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_contact_file_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun handleFileUris(uris: List<Uri>) {
        lifecycleScope.launch {
            runCatching {
                statusDialog = showProcessFileDialog(this@ContactFileListActivity, intent)
                val documents = viewModel.prepareFiles(uris)
                onIntentProcessed(documents)
            }.onFailure {
                dismissAlertDialogIfExists(statusDialog)
                Timber.e(it)
            }
        }
    }

    private fun uploadFilesManually() {
        runCatching {
            openMultipleDocumentLauncher.launch(arrayOf("*/*"))
        }.onFailure {
            Timber.e(it)
        }
    }

    companion object {
        private const val PARENT_HANDLE = "parentHandle"
    }
}
