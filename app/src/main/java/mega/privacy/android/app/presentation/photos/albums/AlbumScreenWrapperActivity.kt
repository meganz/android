package mega.privacy.android.app.presentation.photos.albums

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.albums.coverselection.AlbumCoverSelectionScreen
import mega.privacy.android.app.presentation.photos.albums.decryptionkey.AlbumDecryptionKeyScreen
import mega.privacy.android.app.presentation.photos.albums.getlink.AlbumGetLinkScreen
import mega.privacy.android.app.presentation.photos.albums.getmultiplelinks.AlbumGetMultipleLinksScreen
import mega.privacy.android.app.presentation.photos.albums.importdeeplink.AlbumImportDeeplinkScreen
import mega.privacy.android.app.presentation.photos.albums.importlink.AlbumImportScreen
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumFlow
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumPhotosSelectionScreen
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

@AndroidEntryPoint
class AlbumScreenWrapperActivity : BaseActivity() {
    private enum class AlbumScreen {
        AlbumPhotosSelectionScreen,
        AlbumCoverSelectionScreen,
        AlbumGetLinkScreen,
        AlbumGetMultipleLinksScreen,
        AlbumDecryptionKeyScreen,
        AlbumImportDeeplinkScreen,
        AlbumImportScreen,
    }

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val albumScreen: AlbumScreen? by lazy(LazyThreadSafetyMode.NONE) {
        AlbumScreen.valueOf(intent.getStringExtra(ALBUM_SCREEN) ?: "")
    }

    private val isAlbumNewLink: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        intent.getBooleanExtra(ALBUM_NEW_LINK, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            nodeSaver.restoreState(savedInstanceState)
        }

        setContent {
            val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                when (albumScreen) {
                    AlbumScreen.AlbumPhotosSelectionScreen -> {
                        AlbumPhotosSelectionScreen(
                            onBackClicked = ::finish,
                            onCompletion = { albumId, numCommittedPhotos ->
                                val data = Intent().apply {
                                    putExtra(ALBUM_ID, albumId.id)
                                    putExtra(NUM_PHOTOS, numCommittedPhotos)
                                }
                                setResult(RESULT_OK, data)
                                finish()
                            },
                        )
                    }
                    AlbumScreen.AlbumCoverSelectionScreen -> {
                        AlbumCoverSelectionScreen(
                            onBackClicked = ::finish,
                            onCompletion = { message ->
                                val data = Intent().apply {
                                    putExtra(MESSAGE, message)
                                }
                                setResult(RESULT_OK, data)
                                finish()
                            },
                        )
                    }
                    AlbumScreen.AlbumGetLinkScreen -> {
                        AlbumGetLinkScreen(
                            isAlbumNewLink = isAlbumNewLink,
                            onBack = ::finish,
                            onLearnMore = {
                                val intent = createAlbumDecryptionKeyScreen(this)
                                startActivity(intent)
                            },
                            onShareLink = { link ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, link)
                                }
                                val shareIntent = Intent.createChooser(
                                    intent,
                                    getString(R.string.general_share)
                                )
                                startActivity(shareIntent)
                            },
                        )
                    }
                    AlbumScreen.AlbumGetMultipleLinksScreen -> {
                        AlbumGetMultipleLinksScreen(
                            onBack = ::finish,
                            onShareLinks = { albumLinks ->
                                val linksString = albumLinks.joinToString(System.lineSeparator()) {
                                    it.link
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, linksString)
                                }
                                val shareIntent = Intent.createChooser(
                                    intent,
                                    getString(R.string.general_share)
                                )
                                startActivity(shareIntent)
                            },
                        )
                    }
                    AlbumScreen.AlbumDecryptionKeyScreen -> {
                        AlbumDecryptionKeyScreen(
                            onBack = ::finish,
                        )
                    }
                    AlbumScreen.AlbumImportDeeplinkScreen -> {
                        AlbumImportDeeplinkScreen(
                            onApply = {
                                val intent = createAlbumImportScreen(
                                    context = this,
                                    albumLink = AlbumLink(it),
                                )
                                startActivity(intent)
                                finish()
                            },
                        )
                    }
                    AlbumScreen.AlbumImportScreen -> {
                        AlbumImportScreen(
                            onShareLink = { link ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, link)
                                }
                                val shareIntent = Intent.createChooser(
                                    intent,
                                    getString(R.string.general_share)
                                )
                                startActivity(shareIntent)
                            },
                            onPreviewPhoto = {},
                            onSaveToDevice = { nodes ->
                                checkNotificationsPermission(this)
                                nodeSaver.saveNodes(
                                    nodes = nodes,
                                    highPriority = false,
                                    isFolderLink = false,
                                    fromMediaViewer = false,
                                    needSerialize = true,
                                )
                            },
                            onBack = ::finish,
                        )
                    }
                    else -> finish()
                }
            }
        }
    }

    /**
     * Start: Download node block
     */

    private val nodeSaver = NodeSaver(
        activityLauncher = this,
        permissionRequester = this,
        snackbarShower = this,
        confirmDialogShower = AlertsAndWarnings.showSaveToDeviceConfirmDialog(this),
    )

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        nodeSaver.saveState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        intent ?: return
        nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        nodeSaver.handleRequestPermissionsResult(requestCode)
    }

    override fun onDestroy() {
        nodeSaver.destroy()
        super.onDestroy()
    }

    /**
     * End
     */

    companion object {
        private const val ALBUM_SCREEN: String = "album_screen"

        const val ALBUM_ID: String = "album_id"

        const val ALBUM_FLOW: String = "album_flow"

        const val ALBUM_NEW_LINK: String = "album_new_link"

        const val ALBUM_LINK: String = "album_link"

        const val NUM_PHOTOS: String = "num_photos"

        const val MESSAGE: String = "message"

        fun createAlbumPhotosSelectionScreen(
            context: Context,
            albumId: AlbumId,
            albumFlow: AlbumFlow,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumPhotosSelectionScreen.name)
            putExtra(ALBUM_ID, albumId.id)
            putExtra(ALBUM_FLOW, albumFlow.ordinal)
        }

        fun createAlbumCoverSelectionScreen(
            context: Context,
            albumId: AlbumId,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumCoverSelectionScreen.name)
            putExtra(ALBUM_ID, albumId.id)
        }

        fun createAlbumGetLinkScreen(
            context: Context,
            albumId: AlbumId,
            isNewLink: Boolean,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumGetLinkScreen.name)
            putExtra(ALBUM_ID, albumId.id)
            putExtra(ALBUM_NEW_LINK, isNewLink)
        }

        fun createAlbumGetMultipleLinksScreen(
            context: Context,
            albumIds: Set<AlbumId>,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumGetMultipleLinksScreen.name)
            putExtra(ALBUM_ID, albumIds.map {
                it.id
            }.toLongArray())
        }

        fun createAlbumDecryptionKeyScreen(
            context: Context,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumDecryptionKeyScreen.name)
        }

        fun createAlbumImportDeeplinkScreen(
            context: Context,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumImportDeeplinkScreen.name)
        }

        fun createAlbumImportScreen(
            context: Context,
            albumLink: AlbumLink,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumImportScreen.name)
            putExtra(ALBUM_LINK, albumLink.link)
        }
    }
}
