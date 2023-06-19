package mega.privacy.android.app.presentation.photos.mediadiscovery

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.interfaces.PermissionRequester
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.mediadiscovery.view.MediaDiscoveryScreen
import mega.privacy.android.app.utils.AlertDialogUtil
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaProgressDialogUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.showSnackbar
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * This activity using for handling folder link MD case
 */
@AndroidEntryPoint
class MediaDiscoveryActivity : BaseActivity(), PermissionRequester, SnackbarShower {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val mediaDiscoveryGlobalStateViewModel: MediaDiscoveryGlobalStateViewModel by viewModels()
    private val mediaDiscoveryViewModel: MediaDiscoveryViewModel by viewModels()

    private val folderName: String? by lazy(LazyThreadSafetyMode.NONE) {
        intent.getStringExtra(INTENT_KEY_CURRENT_FOLDER_NAME)
    }

    private val mediaHandle: Long by lazy(LazyThreadSafetyMode.NONE) {
        intent.getLongExtra(INTENT_KEY_CURRENT_FOLDER_ID, 0L)
    }

    private lateinit var selectImportFolderLauncher: ActivityResultLauncher<Intent>
    private var statusDialog: AlertDialog? = null

    private val nodeSaver = NodeSaver(
        this, this, this,
        AlertsAndWarnings.showSaveToDeviceConfirmDialog(this)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            nodeSaver.restoreState(savedInstanceState)
        }
        checkLoginStatus()
        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                MediaDiscoveryScreen(
                    mediaDiscoveryGlobalStateViewModel = mediaDiscoveryGlobalStateViewModel,
                    viewModel = mediaDiscoveryViewModel,
                    screenTitle = folderName,
                    onBackClicked = ::finish,
                    onPhotoClicked = this::onClick,
                    onPhotoLongPressed = this::onLongPress,
                    onImportClicked = this::importNode,
                    onSaveToDeviceClicked = this::saveToDevice,
                )
            }
        }

        setupFlow()
        setupLauncher()
    }

    private fun checkLoginStatus() {
        mediaDiscoveryViewModel.checkLoginRequired()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        nodeSaver.saveState(outState)
    }

    override fun onDestroy() {
        nodeSaver.destroy()
        super.onDestroy()
    }

    private fun setupFlow() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    mediaDiscoveryViewModel.state.collect {
                        if (it.collisions != null) {
                            AlertDialogUtil.dismissAlertDialogIfExists(statusDialog)
                            nameCollisionActivityContract?.launch(it.collisions)
                            mediaDiscoveryViewModel.resetLaunchCollisionActivity()
                        } else if (it.copyResultText != null || it.copyThrowable != null) {
                            showCopyResult(it.copyResultText, it.copyThrowable)
                            mediaDiscoveryViewModel.resetShowCopyResult()
                        }
                    }
                }

                launch {
                    mediaDiscoveryGlobalStateViewModel.state.collect { zoomLevel ->
                        mediaDiscoveryViewModel.updateZoomLevel(zoomLevel)
                    }
                }

                launch {
                    mediaDiscoveryGlobalStateViewModel.filterState.collect { filterType ->
                        mediaDiscoveryViewModel.setCurrentMediaType(filterType)
                    }
                }
            }
        }
    }


    private fun setupLauncher() {
        nameCollisionActivityContract =
            registerForActivityResult(NameCollisionActivityContract()) { result: String? ->
                result?.let {
                    showSnackbar(
                        this@MediaDiscoveryActivity,
                        it
                    )
                }
            }

        selectImportFolderLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            selectImportFolderResult
        )
    }

    @SuppressLint("CheckResult")
    private val selectImportFolderResult =
        ActivityResultCallback<ActivityResult> { activityResult ->
            val resultCode = activityResult.resultCode
            val intent = activityResult.data

            if (resultCode != RESULT_OK || intent == null) {
                return@ActivityResultCallback
            }

            if (!isConnected())
                return@ActivityResultCallback

            val toHandle = intent.getLongExtra("IMPORT_TO", 0)

            statusDialog =
                MegaProgressDialogUtil.createProgressDialog(
                    this,
                    getString(R.string.general_importing)
                )
            statusDialog?.show()

            lifecycleScope.launch {
                val selectedNodes = mediaDiscoveryViewModel.getSelectedNodes()
                if (selectedNodes.isNotEmpty()) {
                    // Import the selected nodes
                    Timber.d("Is multiple select")
                    val authorizedNodes =
                        selectedNodes.mapNotNull { megaApiFolder.authorizeNode(it) }
                    mediaDiscoveryViewModel.checkNameCollision(authorizedNodes, toHandle)
                } else {
                    // No selection, import the whole folder
                    val node = with(megaApiFolder) {
                        authorizeNode(rootNode)
                    }
                    node?.let {
                        mediaDiscoveryViewModel.checkNameCollision(listOf(it), toHandle)
                    }
                }
            }
        }

    private fun isConnected(): Boolean {
        if (!mediaDiscoveryViewModel.isConnected) {
            try {
                statusDialog?.dismiss()
            } catch (exception: Exception) {
                Timber.e(exception)
            }
            showSnackbar(
                this,
                getString(R.string.error_server_connection_problem)
            )
            return false
        }
        return true
    }

    /**
     * Shows the copy Result.
     *
     * @param copyResultText Copy result text.
     * @param throwable
     */

    private fun showCopyResult(copyResultText: String?, throwable: Throwable?) {
        AlertDialogUtil.dismissAlertDialogIfExists(statusDialog)
        when {
            copyResultText != null -> {
                showSnackbar(
                    this@MediaDiscoveryActivity,
                    copyResultText
                )
            }

            throwable != null -> {
                manageCopyMoveException(throwable)
            }

            else -> {
                showSnackbar(
                    this@MediaDiscoveryActivity,
                    getString(R.string.context_correctly_copied)
                )
            }
        }

        mediaDiscoveryViewModel.clearSelectedPhotos()
    }

    private fun openPhoto(photo: Photo) {
        ImageViewerActivity.getIntentForChildren(
            this,
            mediaDiscoveryViewModel.getAllPhotoIds().toLongArray(),
            photo.id,
        ).run {
            startActivity(this)
        }
        overridePendingTransition(0, 0)
    }

    private fun onClick(photo: Photo) {
        if (mediaDiscoveryViewModel.state.value.selectedPhotoIds.isEmpty()) {
            if (photo is Photo.Video) {
                lifecycleScope.launch {
                    launchVideoScreenForFolderLink(photo)
                }
            } else {
                openPhoto(photo)
            }
        } else {
            mediaDiscoveryViewModel.togglePhotoSelection(photo.id)
        }
    }

    private fun onLongPress(photo: Photo) {
        if (mediaDiscoveryViewModel.state.value.selectedPhotoIds.isEmpty()) {
            mediaDiscoveryViewModel.togglePhotoSelection(photo.id)
        } else {
            onClick(photo)
        }
    }

    /**
     * Launch video player for folder link
     *
     * @param photo Photo item
     */
    private suspend fun launchVideoScreenForFolderLink(photo: Photo) {
        val nodeHandle = photo.id
        val nodeName = photo.name
        val intent = Util.getMediaIntent(this, nodeName).apply {
            putExtra(Constants.INTENT_EXTRA_KEY_POSITION, 0)
            putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, nodeHandle)
            putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, nodeName)
            putExtra(
                Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                Constants.FOLDER_LINK_ADAPTER //FolderLink type
            )
            putExtra(
                Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                mediaDiscoveryViewModel.getNodeParentHandle(nodeHandle)
            )
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(
            mediaDiscoveryViewModel.isLocalFile(nodeHandle)?.let { localPath ->
                File(localPath).let { mediaFile ->
                    kotlin.runCatching {
                        FileProvider.getUriForFile(
                            this,
                            Constants.AUTHORITY_STRING_FILE_PROVIDER,
                            mediaFile
                        )
                    }.onFailure {
                        Uri.fromFile(mediaFile)
                    }.map { mediaFileUri ->
                        intent.setDataAndType(
                            mediaFileUri,
                            MimeTypeList.typeForName(nodeName).type
                        )
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                }
                intent
            } ?: let {
                val memoryInfo = ActivityManager.MemoryInfo()
                (this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .getMemoryInfo(memoryInfo)
                mediaDiscoveryViewModel.updateIntent(
                    handle = nodeHandle,
                    name = nodeName,
                    isNeedsMoreBufferSize = memoryInfo.totalMem > Constants.BUFFER_COMP,
                    intent = intent,
                    isFolderLink = true
                )
            }
        )
    }

    /**
     * Handle import option
     */
    private fun importNode() {
        val intent = Intent(this, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
        selectImportFolderLauncher.launch(intent)
    }

    /**
     * Handle download option
     */
    private fun saveToDevice() {
        lifecycleScope.launch {
            val nodes = mediaDiscoveryViewModel.getNodes()
            downloadNodes(nodes)
            mediaDiscoveryViewModel.clearSelectedPhotos()
        }
    }

    private fun downloadNodes(nodes: List<MegaNode>) {
        PermissionUtils.checkNotificationsPermission(this)
        nodeSaver.saveNodes(
            nodes,
            highPriority = false,
            isFolderLink = true,
            fromMediaViewer = false,
            needSerialize = false
        )
    }

    @SuppressLint("CheckResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Timber.d("onActivityResult")
        if (intent == null || nodeSaver.handleActivityResult(this, requestCode, resultCode, intent))
            return
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        nodeSaver.handleRequestPermissionsResult(requestCode)
    }

    companion object {
        @JvmStatic
        fun startMDActivity(
            context: Context,
            mediaHandle: Long,
            folderName: String,
            isOpenByMDIcon: Boolean = false,
        ) {
            val intent = Intent(context, MediaDiscoveryActivity::class.java).apply {
                putExtra(INTENT_KEY_CURRENT_FOLDER_ID, mediaHandle)
                putExtra(INTENT_KEY_CURRENT_FOLDER_NAME, folderName)
                putExtra(INTENT_KEY_FROM_FOLDER_LINK, true)
                putExtra(INTENT_KEY_OPEN_MEDIA_DISCOVERY_BY_MD_ICON, isOpenByMDIcon)
            }
            context.startActivity(intent)
        }

        internal const val INTENT_KEY_CURRENT_FOLDER_ID = "CURRENT_FOLDER_ID"
        internal const val INTENT_KEY_FROM_FOLDER_LINK = "FROM_FOLDER_LINK"
        internal const val INTENT_KEY_CURRENT_FOLDER_NAME = "CURRENT_FOLDER_NAME"
        private const val INTENT_KEY_OPEN_MEDIA_DISCOVERY_BY_MD_ICON =
            "OPEN_MEDIA_DISCOVERY_BY_MD_ICON"
    }
}
