package mega.privacy.android.app.presentation.photos.mediadiscovery

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.interfaces.PermissionRequester
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.FolderLinkMediaDiscoveryImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.photos.mediadiscovery.view.MediaDiscoveryScreen
import mega.privacy.android.app.utils.AlertDialogUtil
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.MegaProgressDialogUtil
import mega.privacy.android.app.utils.Util.showSnackbar
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
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

    /**
     * [MegaNavigator] injection
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

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
    private val nameCollisionActivityLauncher = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        result?.let {
            showSnackbar(this@MediaDiscoveryActivity, it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        checkLoginStatus()
        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                MediaDiscoveryScreen(
                    mediaDiscoveryGlobalStateViewModel = mediaDiscoveryGlobalStateViewModel,
                    viewModel = mediaDiscoveryViewModel,
                    screenTitle = folderName,
                    onBackClicked = ::finish,
                    onPhotoClicked = this::onClick,
                    onPhotoLongPressed = this::onLongPress,
                    onImportClicked = this::importNode,
                )
            }
        }

        setupFlow()
        setupLauncher()
    }

    private fun checkLoginStatus() {
        mediaDiscoveryViewModel.checkLoginRequired()
    }

    private fun setupFlow() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    mediaDiscoveryViewModel.state.collect {
                        if (it.collisions.isNotEmpty()) {
                            AlertDialogUtil.dismissAlertDialogIfExists(statusDialog)
                            nameCollisionActivityLauncher.launch(ArrayList(it.collisions))
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
                val selectedNodes = mediaDiscoveryViewModel.getSelectedNodes().let {
                    // When there are no selected nodes, import the whole folder
                    it.ifEmpty { listOf(megaApiFolder.rootNode) }
                }.mapNotNull { it }
                mediaDiscoveryViewModel.checkNameCollision(
                    nodeHandles = selectedNodes.map { it.handle },
                    toHandle = toHandle
                )
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
        lifecycleScope.launch {
            ImagePreviewActivity.createIntent(
                context = this@MediaDiscoveryActivity,
                imageSource = ImagePreviewFetcherSource.FOLDER_LINK_MEDIA_DISCOVERY,
                menuOptionsSource = ImagePreviewMenuSource.FOLDER_LINK,
                anchorImageNodeId = NodeId(photo.id),
                isForeign = true,
                params = mapOf(
                    FolderLinkMediaDiscoveryImageNodeFetcher.PARENT_ID to mediaHandle,
                ),
            ).run {
                startActivity(this)
            }
        }
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
        runCatching {
            mediaDiscoveryViewModel.isLocalFile(nodeHandle)?.let { localPath ->
                val file = File(localPath)
                megaNavigator.openMediaPlayerActivityByLocalFile(
                    context = this,
                    localFile = file,
                    handle = nodeHandle,
                    parentId = mediaDiscoveryViewModel.getNodeParentHandle(nodeHandle) ?: -1,
                    viewType = FOLDER_LINK_ADAPTER,
                    isFolderLink = true
                )
            } ?: run {
                val contentUri = mediaDiscoveryViewModel.getNodeContentUri(nodeHandle)
                megaNavigator.openMediaPlayerActivity(
                    context = this,
                    contentUri = contentUri,
                    name = nodeName,
                    handle = nodeHandle,
                    parentId = mediaDiscoveryViewModel.getNodeParentHandle(nodeHandle) ?: -1,
                    viewType = FOLDER_LINK_ADAPTER,
                    isFolderLink = true
                )
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Handle import option
     */
    private fun importNode() {
        val intent = Intent(this, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
        selectImportFolderLauncher.launch(intent)
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
