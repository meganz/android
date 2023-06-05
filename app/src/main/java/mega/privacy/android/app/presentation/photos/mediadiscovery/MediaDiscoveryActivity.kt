package mega.privacy.android.app.presentation.photos.mediadiscovery

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.mediadiscovery.view.MediaDiscoveryScreen
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetThemeMode
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MediaDiscoveryActivity : AppCompatActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val mediaDiscoveryGlobalStateViewModel: MediaDiscoveryGlobalStateViewModel by viewModels()
    private val mediaDiscoveryViewModel: MediaDiscoveryViewModel by viewModels()

    private val folderName: String? by lazy(LazyThreadSafetyMode.NONE) {
        intent.getStringExtra(INTENT_KEY_CURRENT_FOLDER_NAME)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                )
            }
        }

        setupFlow()
    }

    private fun setupFlow() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
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
                    launchVideoScreen(photo)
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
     * Launch video player
     *
     * @param photo Photo item
     */
    private suspend fun launchVideoScreen(photo: Photo) {
        val nodeHandle = photo.id
        val nodeName = photo.name
        val intent = Util.getMediaIntent(this, nodeName).apply {
            putExtra(Constants.INTENT_EXTRA_KEY_POSITION, 0)
            putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, nodeHandle)
            putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, nodeName)
            putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.FROM_MEDIA_DISCOVERY)
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
                        intent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(nodeName).type)
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
                )
            }
        )
    }


    /**
     * Handle import option
     */
    fun importNode() {
        //TODO
    }

    /**
     * Handle download option
     */
    fun downloadNode() {
        //TODO
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
                putExtra(INTENT_KEY_OPEN_MEDIA_DISCOVERY_BY_MD_ICON, isOpenByMDIcon)
            }
            context.startActivity(intent)
        }

        internal const val INTENT_KEY_CURRENT_FOLDER_ID = "CURRENT_FOLDER_ID"
        internal const val INTENT_KEY_CURRENT_FOLDER_NAME = "CURRENT_FOLDER_NAME"
        private const val INTENT_KEY_OPEN_MEDIA_DISCOVERY_BY_MD_ICON =
            "OPEN_MEDIA_DISCOVERY_BY_MD_ICON"
    }
}
