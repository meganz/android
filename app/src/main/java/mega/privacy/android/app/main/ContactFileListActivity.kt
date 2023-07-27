package mega.privacy.android.app.main

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.R
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_DESTROY_ACTION_MODE
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_MANAGE_SHARE
import mega.privacy.android.app.generalusecase.FilePrepareUseCase
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.modalbottomsheet.ContactFileListBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollision.Upload.Companion.getUploadCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.bottomsheet.UploadBottomSheetDialogActionListener
import mega.privacy.android.app.presentation.contact.ContactFileListViewModel
import mega.privacy.android.app.presentation.copynode.CopyRequestResult
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.extensions.uploadFilesManually
import mega.privacy.android.app.presentation.extensions.uploadFolderManually
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.LegacyCopyNodeUseCase
import mega.privacy.android.app.usecase.UploadUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException.ChildDoesNotExistsException
import mega.privacy.android.app.usecase.exception.MegaNodeException.ParentDoesNotExistException
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.Constants
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
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.UploadUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getAudioPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getImagePermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getReadExternalStoragePermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getVideoPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import nz.mega.documentscanner.DocumentScannerActivity
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
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class ContactFileListActivity : PasscodeActivity(), MegaGlobalListenerInterface,
    MegaRequestListenerInterface, UploadBottomSheetDialogActionListener, ActionNodeCallback,
    SnackbarShower {

    @Inject
    lateinit var filePrepareUseCase: FilePrepareUseCase

    @Inject
    lateinit var getNodeUseCase: GetNodeUseCase

    @Inject
    lateinit var checkNameCollisionUseCase: CheckNameCollisionUseCase

    @Inject
    lateinit var uploadUseCase: UploadUseCase

    @Inject
    lateinit var legacyCopyNodeUseCase: LegacyCopyNodeUseCase

    @Inject
    lateinit var copyRequestMessageMapper: CopyRequestMessageMapper

    @Inject
    lateinit var moveRequestMessageMapper: MoveRequestMessageMapper
    private val viewModel: ContactFileListViewModel by viewModels()
    private lateinit var fragmentContainer: FrameLayout
    private var userEmail: String? = null
    private var contact: MegaUser? = null
    private var fullName = ""
    private var contactFileListFragment: ContactFileListFragment? = null
    private val nodeSaver = NodeSaver(
        this, this, this,
        showSaveToDeviceConfirmDialog(this)
    )
    private var parentHandle: Long = -1
    private var newFolderDialog: AlertDialog? = null
    private var statusDialog: AlertDialog? = null
    var selectedNode: MegaNode? = null
    private var tB: Toolbar? = null
    private var aB: ActionBar? = null
    private var bottomSheetDialogFragment: BottomSheetDialogFragment? = null
    private var newTextFileDialog: AlertDialog? = null
    private val manageShareReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null) return
            contactFileListFragment?.takeIf { it.isVisible }?.let {
                it.clearSelections()
                it.hideMultipleSelect()
            }
            statusDialog?.dismiss()
        }
    }
    private val destroyActionModeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action == null || intent.action != BROADCAST_ACTION_DESTROY_ACTION_MODE) return
            contactFileListFragment?.takeIf { it.isVisible }?.let {
                it.clearSelections()
                it.hideMultipleSelect()
            }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(PARENT_HANDLE, parentHandle)
        checkNewTextFileDialogState(newTextFileDialog, outState)
        nodeSaver.saveState(outState)
        newFolderDialog.checkNewFolderDialogState(outState)
        super.onSaveInstanceState(outState)
    }

    /**
     * When manually uploading Files and the device is running Android 13 and above, this Launcher
     * is called to request the Notification Permission (if possible) and upload Files regardless
     * if the Notification Permission is granted or not
     */
    private val manualUploadFilesLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _: Boolean? -> this.uploadFilesManually() }

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
            this.uploadFilesManually()
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
        val saveDestinations = arrayOf(
            getString(R.string.section_cloud_drive),
            getString(R.string.section_chat)
        )
        val intent = DocumentScannerActivity.getIntent(this, saveDestinations)
        startActivityForResult(intent, Constants.REQUEST_CODE_SCAN_DOCUMENT)
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
                Constants.SNACKBAR_TYPE,
                getString(R.string.error_server_connection_problem)
            )
            return
        }
        if (isFinishing) {
            return
        }
        val parentHandle = contactFileListFragment?.getParentHandle() ?: return
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
                Constants.SNACKBAR_TYPE,
                getString(R.string.context_folder_already_exists)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        Timber.d("onCreate first")
        super.onCreate(savedInstanceState)
        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return
        }
        if (savedInstanceState == null) {
            setParentHandle(-1)
        } else {
            setParentHandle(savedInstanceState.getLong(PARENT_HANDLE, -1))
            nodeSaver.restoreState(savedInstanceState)
        }
        megaApi.addGlobalListener(this)
        registerReceiver(manageShareReceiver, IntentFilter(BROADCAST_ACTION_INTENT_MANAGE_SHARE))
        registerReceiver(
            destroyActionModeReceiver,
            IntentFilter(BROADCAST_ACTION_DESTROY_ACTION_MODE)
        )
        val extras = intent.extras
        if (extras != null) {
            userEmail = extras.getString(Constants.NAME)
            val currNodePosition = extras.getInt("node_position", -1)
            setContentView(R.layout.activity_main_contact_properties)
            val coordinatorLayout =
                findViewById<View>(R.id.contact_properties_main_activity_layout) as CoordinatorLayout
            coordinatorLayout.fitsSystemWindows = false

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
            coordinatorLayout.fitsSystemWindows = true
            contactFileListFragment =
                supportFragmentManager.findFragmentByTag("cflF") as ContactFileListFragment?
            if (contactFileListFragment == null) {
                contactFileListFragment = ContactFileListFragment()
            }
            contactFileListFragment?.let {
                it.setUserEmail(userEmail)
                it.setCurrNodePosition(currNodePosition)
                it.setParentHandle(parentHandle)
                supportFragmentManager.beginTransaction().replace(
                    R.id.fragment_container_contact_properties,
                    it,
                    "cflF"
                ).commitNow()
            }
            coordinatorLayout.invalidate()
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
        }
        collectFlows()
    }

    private fun collectFlows() {
        collectFlow(viewModel.state) { uiState ->
            val nodeNameCollisionResult = uiState.nodeNameCollisionResult
            if (nodeNameCollisionResult != null) {
                handleNodesNameCollisionResult(nodeNameCollisionResult)
                viewModel.markHandleNodeNameCollisionResult()
            }
            if (uiState.moveRequestResult != null) {
                handleMovementResult(uiState.moveRequestResult)
                viewModel.markHandleMoveRequestResult()
            }
        }
    }

    private fun handleMovementResult(moveRequestResult: Result<MoveRequestResult>) {
        dismissAlertDialogIfExists(statusDialog)
        if (moveRequestResult.isSuccess) {
            val data = moveRequestResult.getOrThrow()
            if (data !is MoveRequestResult.DeleteMovement) {
                showMovementResult(data, data.nodes.first())
            }
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                moveRequestMessageMapper(data),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
        } else {
            manageCopyMoveException(moveRequestResult.exceptionOrNull())
        }
    }

    private fun handleNodesNameCollisionResult(result: NodeNameCollisionResult) {
        if (result.conflictNodes.isNotEmpty()) {
            dismissAlertDialogIfExists(statusDialog)
            nameCollisionActivityContract
                ?.launch(ArrayList(result.conflictNodes.values.map {
                    NameCollision.Movement.getMovementCollision(it)
                }))
        }
        if (result.noConflictNodes.isNotEmpty()) {
            if (result.type == NodeNameCollisionType.MOVE) {
                viewModel.moveNodes(result.noConflictNodes)
            }
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
        nodeSaver.handleRequestPermissionsResult(requestCode)
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
        unregisterReceiver(manageShareReceiver)
        unregisterReceiver(destroyActionModeReceiver)
        dismissAlertDialogIfExists(newFolderDialog)
        nodeSaver.destroy()
    }

    fun setParentHandle(parentHandle: Long) {
        this.parentHandle = parentHandle
    }

    fun downloadFile(nodes: List<MegaNode>) {
        checkNotificationsPermission(this)
        nodeSaver.saveNodes(
            nodes = nodes,
            highPriority = true,
            isFolderLink = false,
            fromMediaViewer = false,
            needSerialize = false
        )
    }

    private fun moveToTrash(handleList: ArrayList<Long>) {
        Timber.d("moveToTrash: ")
        if (!viewModel.isOnline()) {
            showSnackbar(
                Constants.SNACKBAR_TYPE,
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
    fun showMovementResult(result: MoveRequestResult, handle: Long) {
        dismissAlertDialogIfExists(statusDialog)
        actionConfirmed()
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
        val intent = Intent(this, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_COPY_FOLDER
        intent.putExtra("COPY_FROM", handleList.toLongArray())
        startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_FOLDER_TO_COPY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
            return
        }
        if (requestCode == Constants.REQUEST_CODE_SELECT_FOLDER_TO_COPY && resultCode == RESULT_OK) {
            if (intent == null) {
                return
            }
            if (!viewModel.isOnline()) {
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem)
                )
                return
            }
            val temp: AlertDialog
            try {
                temp = createProgressDialog(
                    this,
                    getString(R.string.context_copying)
                )
                temp.show()
            } catch (e: Exception) {
                return
            }
            statusDialog = temp
            val copyHandles = intent.getLongArrayExtra("COPY_HANDLES")
            val toHandle = intent.getLongExtra("COPY_TO", 0)
            if (copyHandles == null || copyHandles.isEmpty()) return
            composite.add(
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
                                composite.add(
                                    legacyCopyNodeUseCase.copy(handlesWithoutCollision, toHandle)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe { copyResult: CopyRequestResult?, copyThrowable: Throwable? ->
                                            dismissAlertDialogIfExists(statusDialog)
                                            contactFileListFragment?.takeIf { it.isVisible }?.let {
                                                it.clearSelections()
                                                it.hideMultipleSelect()
                                            }
                                            copyThrowable?.let { manageCopyMoveException(it) }
                                                ?: showSnackbar(
                                                    Constants.SNACKBAR_TYPE,
                                                    copyRequestMessageMapper.invoke(copyResult),
                                                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                                                )
                                        })
                            }
                        }
                    })
        } else if (requestCode == Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE && resultCode == RESULT_OK) {
            if (intent == null) {
                return
            }
            if (!viewModel.isOnline()) {
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem)
                )
                return
            }
            val moveHandles = intent.getLongArrayExtra("MOVE_HANDLES")
            val toHandle = intent.getLongExtra("MOVE_TO", 0)
            val temp: AlertDialog
            try {
                temp = createProgressDialog(
                    this,
                    getString(R.string.context_moving)
                )
                temp.show()
            } catch (e: Exception) {
                return
            }
            statusDialog = temp
            if (moveHandles == null || moveHandles.isEmpty()) return
            viewModel.checkMoveNodesNameCollision(moveHandles.toList(), toHandle)
        } else if (requestCode == Constants.REQUEST_CODE_GET_FILES && resultCode == RESULT_OK) {
            if (intent == null) {
                return
            }
            intent.action = Intent.ACTION_GET_CONTENT
            try {
                statusDialog = createProgressDialog(
                    this,
                    resources.getQuantityString(
                        R.plurals.upload_prepare,
                        1
                    )
                )?.also { it.show() }
            } catch (e: Exception) {
                return
            }
            composite.add(
                filePrepareUseCase.prepareFiles(intent)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { shareInfo: List<ShareInfo>?, throwable: Throwable? ->
                        if (throwable == null) {
                            onIntentProcessed(shareInfo)
                        }
                    })
        } else if (requestCode == Constants.REQUEST_CODE_GET_FOLDER) {
            UploadUtil.getFolder(this, resultCode, intent, parentHandle)
        } else if (requestCode == Constants.REQUEST_CODE_GET_FOLDER_CONTENT) {
            if (intent != null && resultCode == RESULT_OK) {
                val result = intent.getStringExtra(Constants.EXTRA_ACTION_RESULT)
                if (TextUtil.isTextEmpty(result)) {
                    return
                }
                showSnackbar(Constants.SNACKBAR_TYPE, result)
            }
        } else if (requestCode == Constants.TAKE_PHOTO_CODE) {
            Timber.d("TAKE_PHOTO_CODE")
            if (resultCode == RESULT_OK) {
                val parentHandle = contactFileListFragment?.getParentHandle() ?: return
                val file = UploadUtil.getTemporalTakePictureFile(this)
                if (file != null) {
                    composite.add(
                        checkNameCollisionUseCase.check(file.name, parentHandle)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                { handle: Long ->
                                    val list = ArrayList<NameCollision>()
                                    list.add(
                                        getUploadCollision(
                                            handle,
                                            file, parentHandle
                                        )
                                    )
                                    nameCollisionActivityContract?.launch(list)
                                }
                            ) { throwable: Throwable ->
                                if (throwable is ParentDoesNotExistException) {
                                    showSnackbar(
                                        Constants.SNACKBAR_TYPE,
                                        getString(R.string.general_error)
                                    )
                                } else if (throwable is ChildDoesNotExistsException) {
                                    checkNotificationsPermission(this)
                                    composite.add(
                                        uploadUseCase.upload(
                                            this,
                                            file,
                                            contactFileListFragment?.getParentHandle()
                                                ?: return@subscribe
                                        ).subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe({ Timber.d("Upload started") }) { t: Throwable? ->
                                                Timber.e(t)
                                            })
                                }
                            })
                }
            } else {
                Timber.w("TAKE_PHOTO_CODE--->ERROR!")
            }
        } else if (requestCode == Constants.REQUEST_CODE_SCAN_DOCUMENT && resultCode == RESULT_OK && intent != null) {
            val savedDestination =
                intent.getStringExtra(DocumentScannerActivity.EXTRA_PICKED_SAVE_DESTINATION)
            val fileIntent = Intent(this, FileExplorerActivity::class.java)
            if (getString(R.string.section_chat) == savedDestination) {
                fileIntent.action = FileExplorerActivity.ACTION_UPLOAD_TO_CHAT
            } else {
                fileIntent.action = FileExplorerActivity.ACTION_SAVE_TO_CLOUD
                fileIntent.putExtra(FileExplorerActivity.EXTRA_PARENT_HANDLE, getParentHandle())
            }
            fileIntent.putExtra(Intent.EXTRA_STREAM, intent.data)
            fileIntent.type = intent.type
            startActivity(fileIntent)
        }
    }

    /**
     * Handle processed upload intent.
     *
     * @param infos List<ShareInfo> containing all the upload info.
    </ShareInfo> */
    private fun onIntentProcessed(infos: List<ShareInfo>?) {
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
        if (infos == null) {
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
        composite.add(
            checkNameCollisionUseCase.checkShareInfoList(infos, parentNode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { (collisions, withoutCollisions): Pair<ArrayList<NameCollision>, List<ShareInfo>>, throwable: Throwable? ->
                    dismissAlertDialogIfExists(statusDialog)
                    if (throwable != null) {
                        Util.showErrorAlertDialog(
                            getString(R.string.error_temporary_unavaible),
                            false,
                            this
                        )
                    } else {
                        if (collisions.isNotEmpty()) {
                            nameCollisionActivityContract?.launch(collisions)
                        }
                        if (withoutCollisions.isNotEmpty()) {
                            val text = resources.getQuantityString(
                                R.plurals.upload_began,
                                withoutCollisions.size,
                                withoutCollisions.size
                            )
                            composite.add(
                                uploadUseCase.uploadInfos(
                                    this,
                                    withoutCollisions,
                                    null,
                                    parentNode.handle
                                )
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({
                                        showSnackbar(
                                            Constants.SNACKBAR_TYPE,
                                            text
                                        )
                                    }) { t: Throwable? -> Timber.e(t) })
                        }
                    }
                })
    }

    override fun onBackPressed() {
        if (psaWebBrowser?.consumeBack() == true) return
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
                composite.add(
                    getNodeUseCase.get(parentHandle)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { result: MegaNode?, throwable: Throwable? ->
                            if (throwable == null) {
                                updateNodes()
                            } else {
                                finish()
                            }
                        })
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

    override fun onReloadNeeded(api: MegaApiJava) {}
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
                R.string.general_cancel,
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
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.context_folder_created)
                    )
                    contactFileListFragment?.navigateToFolder(folderNode)
                }
            } else {
                if (contactFileListFragment?.isVisible == true) {
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
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

    fun getParentHandle(): Long = contactFileListFragment?.getParentHandle() ?: -1

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

    companion object {
        private const val PARENT_HANDLE = "parentHandle"
    }
}