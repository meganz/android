package mega.privacy.android.app.presentation.photos.albums

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.getLink.GetLinkViewModel
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.photos.albums.coverselection.AlbumCoverSelectionScreen
import mega.privacy.android.app.presentation.photos.albums.decryptionkey.AlbumDecryptionKeyScreen
import mega.privacy.android.app.presentation.photos.albums.getlink.AlbumGetLinkScreen
import mega.privacy.android.app.presentation.photos.albums.getmultiplelinks.AlbumGetMultipleLinksScreen
import mega.privacy.android.app.presentation.photos.albums.importlink.AlbumImportScreen
import mega.privacy.android.app.presentation.photos.albums.importlink.AlbumImportViewModel
import mega.privacy.android.app.presentation.photos.albums.importlink.ImagePreviewProvider
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumFlow
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumPhotosSelectionScreen
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.settings.model.storageTargetPreference
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

@AndroidEntryPoint
class AlbumScreenWrapperActivity : BaseActivity() {
    private enum class AlbumScreen {
        AlbumPhotosSelectionScreen,
        AlbumCoverSelectionScreen,
        AlbumGetLinkScreen,
        AlbumGetMultipleLinksScreen,
        AlbumDecryptionKeyScreen,
        AlbumImportScreen,
    }

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    private val albumScreen: AlbumScreen? by lazy(LazyThreadSafetyMode.NONE) {
        AlbumScreen.valueOf(intent.getStringExtra(ALBUM_SCREEN) ?: "")
    }

    private val getLinkViewModel: GetLinkViewModel by viewModels()

    @Inject
    lateinit var imagePreviewProvider: ImagePreviewProvider

    /**
     * Mega navigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by monitorThemeModeUseCase().collectAsState(initial = ThemeMode.System)
            OriginalTheme(isDark = themeMode.isDarkMode()) {
                PasscodeContainer(
                    passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                    content = { PsaContainer { AlbumScreenContent() } }
                )
            }
        }
    }

    @Composable
    private fun AlbumScreenContent() {
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
                    fileTypeIconMapper = fileTypeIconMapper
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
                    fileTypeIconMapper = fileTypeIconMapper
                )
            }

            AlbumScreen.AlbumGetLinkScreen -> {
                AlbumGetLinkScreen(
                    getLinkViewModel = getLinkViewModel,
                    createView = ::showFragment,
                    onBack = ::finish,
                    onLearnMore = {
                        val intent = createAlbumDecryptionKeyScreen(this)
                        startActivity(intent)
                    },
                    onShareLink = { album, link ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, album?.title.orEmpty())
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
                    createView = ::showFragment,
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

            AlbumScreen.AlbumImportScreen -> {
                AlbumImportScreen(
                    albumImportViewModel = albumImportViewModel,
                    onShareLink = { link ->
                        if (getStorageState() == StorageState.PayWall) {
                            showOverDiskQuotaPaywallWarning()
                        } else {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, link)
                            }
                            val shareIntent = Intent.createChooser(
                                intent,
                                getString(R.string.general_share)
                            )
                            startActivity(shareIntent)
                        }
                    },
                    onPreviewPhoto = {
                        imagePreviewProvider.onPreviewPhoto(
                            activity = this,
                            photo = it,
                        )
                    },
                    onNavigateFileExplorer = {
                        val intent = Intent(this, FileExplorerActivity::class.java).apply {
                            action = FileExplorerActivity.ACTION_IMPORT_ALBUM
                        }
                        selectFolderLauncher.launch(intent)
                    },
                    onUpgradeAccount = {
                        megaNavigator.openUpgradeAccount(context = this)
                    },
                    onBack = { isBackToHome ->
                        if (isBackToHome) {
                            val intent = Intent(this, ManagerActivity::class.java)
                            startActivity(intent)
                        }
                        finish()
                    },
                    navigateToStorageSettings = {
                        megaNavigator.openSettings(
                            this,
                            storageTargetPreference
                        )
                    }
                )
            }

            else -> finish()
        }
    }

    private fun showFragment(fragment: Fragment): View {
        val containerId = R.id.container
        val containerView = FragmentContainerView(this).apply {
            id = containerId
        }

        supportFragmentManager.beginTransaction()
            .replace(containerId, fragment, fragment.javaClass.simpleName)
            .commitAllowingStateLoss()

        return containerView
    }

    /**
     * Start: Import album block
     */

    private val albumImportViewModel: AlbumImportViewModel by viewModels()

    private val selectFolderLauncher =
        registerForActivityResult(StartActivityForResult()) exit@{ result ->
            val resultCode = result.resultCode
            val data = result.data

            if (resultCode != RESULT_OK || data == null) return@exit

            val folderHandle = data.getLongExtra("IMPORT_TO", 0)
            albumImportViewModel.importAlbum(targetParentFolderNodeId = NodeId(folderHandle))
        }

    /**
     * End
     */

    companion object {
        private const val ALBUM_SCREEN: String = "album_screen"

        const val ALBUM_ID: String = "album_id"

        const val ALBUM_FLOW: String = "album_flow"

        const val ALBUM_LINK: String = "album_link"

        const val NUM_PHOTOS: String = "num_photos"

        const val MESSAGE: String = "message"

        const val HAS_SENSITIVE_ELEMENT: String = "has_sensitive_element"

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
            hasSensitiveElement: Boolean,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumGetLinkScreen.name)
            putExtra(ALBUM_ID, albumId.id)
            putExtra(HAS_SENSITIVE_ELEMENT, hasSensitiveElement)
        }

        fun createAlbumGetMultipleLinksScreen(
            context: Context,
            albumIds: Set<AlbumId>,
            hasSensitiveElement: Boolean,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumGetMultipleLinksScreen.name)
            putExtra(ALBUM_ID, albumIds.map { it.id }.toLongArray())
            putExtra(HAS_SENSITIVE_ELEMENT, hasSensitiveElement)
        }

        fun createAlbumDecryptionKeyScreen(
            context: Context,
        ) = Intent(context, AlbumScreenWrapperActivity::class.java).apply {
            putExtra(ALBUM_SCREEN, AlbumScreen.AlbumDecryptionKeyScreen.name)
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
