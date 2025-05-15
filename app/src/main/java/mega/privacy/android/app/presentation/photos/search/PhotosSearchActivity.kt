package mega.privacy.android.app.presentation.photos.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.DefaultImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.meeting.chat.extension.getInfo
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.photos.PhotoDownloaderViewModel
import mega.privacy.android.app.presentation.photos.PhotosCache
import mega.privacy.android.app.presentation.search.model.navigation.removeNodeLinkDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.cannotOpenFileDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.changeLabelBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.changeNodeExtensionDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.foreignNodeDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.moveToRubbishOrDeleteNavigation
import mega.privacy.android.app.presentation.search.navigation.nodeBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.nodeBottomSheetRoute
import mega.privacy.android.app.presentation.search.navigation.overQuotaDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.renameDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.searchForeignNodeDialog
import mega.privacy.android.app.presentation.search.navigation.searchOverQuotaDialog
import mega.privacy.android.app.presentation.search.searchRoute
import mega.privacy.android.app.presentation.settings.model.StorageTargetPreference
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarShower
import mega.privacy.android.app.presentation.transfers.TransfersManagementViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.presentation.transfers.view.ACTIVE_TAB_INDEX
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Album.FavouriteAlbum
import mega.privacy.android.domain.entity.photos.Album.GifAlbum
import mega.privacy.android.domain.entity.photos.Album.RawAlbum
import mega.privacy.android.domain.entity.photos.Album.UserAlbum
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetLayout
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(ExperimentalMaterialNavigationApi::class)
internal class PhotosSearchActivity : AppCompatActivity(), MegaSnackbarShower {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    @Inject
    lateinit var megaNavigator: MegaNavigator

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    @Inject
    lateinit var listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper

    private val photosSearchViewModel: PhotosSearchViewModel by viewModels()

    private val photoDownloaderViewModel: PhotoDownloaderViewModel by viewModels()

    private val nodeActionsViewModel: NodeActionsViewModel by viewModels()

    private val transfersManagementViewModel: TransfersManagementViewModel by viewModels()

    private val snackbarHostState = SnackbarHostState()

    private val nameCollisionLauncher: ActivityResultLauncher<ArrayList<NameCollision>> =
        registerForActivityResult(
            NameCollisionActivityContract(),
            ::handleNameCollisionResult,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photosSearchViewModel.initialize(
            albumsFlow = PhotosCache.albumsFlow,
            photosFlow = PhotosCache.photosFlow,
        )

        val bottomSheetActionHandler = NodeActionHandler(this, nodeActionsViewModel)

        setContent {
            val theme by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            OriginalTheme(isDark = theme.isDarkMode()) {
                val bottomSheetNavigator = rememberBottomSheetNavigator()
                val navController = rememberNavController(bottomSheetNavigator)
                val scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState)

                val nodeActionState by nodeActionsViewModel.state.collectAsStateWithLifecycle()

                MegaBottomSheetLayout(
                    bottomSheetNavigator = bottomSheetNavigator,
                    modifier = Modifier.navigationBarsPadding(),
                    content = {
                        NavHost(
                            navController = navController,
                            startDestination = searchRoute,
                        ) {
                            composable(searchRoute) {
                                PhotosSearchScreen(
                                    photosSearchViewModel = photosSearchViewModel,
                                    photoDownloaderViewModel = photoDownloaderViewModel,
                                    transfersManagementViewModel = transfersManagementViewModel,
                                    scaffoldState = scaffoldState,
                                    onOpenAlbum = ::openAlbum,
                                    onOpenImagePreviewScreen = ::openImagePreview,
                                    onOpenTransfersScreen = ::openTransfersScreen,
                                    onShowMoreMenu = { nodeId ->
                                        val route =
                                            "$nodeBottomSheetRoute/${nodeId.longValue}/${NodeSourceType.OTHER.name}"
                                        navController.navigate(route)
                                    },
                                    onCloseScreen = ::finish,
                                )
                            }

                            nodeBottomSheetNavigation(
                                nodeActionHandler = bottomSheetActionHandler,
                                navHostController = navController,
                                fileTypeIconMapper = fileTypeIconMapper,
                            )

                            cannotOpenFileDialogNavigation(
                                navHostController = navController,
                                nodeActionsViewModel = nodeActionsViewModel,
                            )

                            foreignNodeDialogNavigation(navController)

                            overQuotaDialogNavigation(navController)

                            changeLabelBottomSheetNavigation(navController)

                            renameDialogNavigation(navController)

                            changeNodeExtensionDialogNavigation(navController)

                            removeNodeLinkDialogNavigation(
                                navHostController = navController,
                                listToStringWithDelimitersMapper = listToStringWithDelimitersMapper,
                            )

                            moveToRubbishOrDeleteNavigation(
                                navHostController = navController,
                                listToStringWithDelimitersMapper = listToStringWithDelimitersMapper,
                            )
                        }
                    },
                )

                StartTransferComponent(
                    event = nodeActionState.downloadEvent,
                    onConsumeEvent = nodeActionsViewModel::markDownloadEventConsumed,
                    snackBarHostState = snackbarHostState,
                    navigateToStorageSettings = {
                        megaNavigator.openSettings(
                            this,
                            StorageTargetPreference
                        )
                    },
                )

                EventEffect(
                    event = nodeActionState.nodeNameCollisionsResult,
                    onConsumed = nodeActionsViewModel::markHandleNodeNameCollisionResult,
                    action = ::handleNodesNameCollisionResult,
                )

                EventEffect(
                    event = nodeActionState.showForeignNodeDialog,
                    onConsumed = nodeActionsViewModel::markForeignNodeDialogShown,
                    action = { navController.navigate(searchForeignNodeDialog) },
                )

                EventEffect(
                    event = nodeActionState.showQuotaDialog,
                    onConsumed = nodeActionsViewModel::markQuotaDialogShown,
                    action = { navController.navigate(searchOverQuotaDialog.plus("/${it}")) },
                )

                EventEffect(
                    event = nodeActionState.infoToShowEvent,
                    onConsumed = nodeActionsViewModel::onInfoToShowEventConsumed,
                    action = { info ->
                        info?.getInfo(this)?.let { text ->
                            scaffoldState.snackbarHostState.showAutoDurationSnackbar(text)
                        }
                    },
                )
            }
        }
    }

    override fun showMegaSnackbar(
        message: String,
        actionLabel: String?,
        duration: MegaSnackbarDuration,
    ) {
        lifecycleScope.launch {
            snackbarHostState.showAutoDurationSnackbar(
                message = message,
                actionLabel = actionLabel,
            )
        }
    }

    private fun openImagePreview(photo: Photo) {
        val photoIds = photosSearchViewModel.state.value.photos.map { it.id }.toLongArray()
        val intent = ImagePreviewActivity.createIntent(
            context = this,
            imageSource = ImagePreviewFetcherSource.DEFAULT,
            menuOptionsSource = ImagePreviewMenuSource.DEFAULT,
            anchorImageNodeId = NodeId(photo.id),
            params = mapOf(DefaultImageNodeFetcher.NODE_IDS to photoIds),
        )
        startActivity(intent)
    }

    private fun openAlbum(album: Album) {
        val data = Intent().apply {
            val bundle = bundleOf(
                "type" to when (album) {
                    FavouriteAlbum -> "favourite"

                    GifAlbum -> "gif"

                    RawAlbum -> "raw"

                    else -> "custom"
                },
                "id" to if (album is UserAlbum) album.id.id else null,
            )
            putExtras(bundle)
        }
        setResult(RESULT_OK, data)
        finish()
    }

    private fun openTransfersScreen() {
        lifecycleScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.TransfersSection)) {
                megaNavigator.openTransfers(this@PhotosSearchActivity, ACTIVE_TAB_INDEX)
            } else {
                startActivity(
                    Intent(this@PhotosSearchActivity, ManagerActivity::class.java)
                        .setAction(Constants.ACTION_SHOW_TRANSFERS)
                        .putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            }
        }
    }

    private fun handleNodesNameCollisionResult(result: NodeNameCollisionsResult) {
        if (result.conflictNodes.isNotEmpty()) {
            nameCollisionLauncher.launch(result.conflictNodes.values.toCollection(ArrayList()))
        }

        if (result.noConflictNodes.isNotEmpty()) {
            when (result.type) {
                NodeNameCollisionType.MOVE -> nodeActionsViewModel.moveNodes(result.noConflictNodes)

                NodeNameCollisionType.COPY -> nodeActionsViewModel.copyNodes(result.noConflictNodes)

                else -> {}
            }
        }
    }

    private fun handleNameCollisionResult(result: String?) {
        result ?: return
        lifecycleScope.launch {
            snackbarHostState.showAutoDurationSnackbar(result)
        }
    }

    override fun onPause() {
        photosSearchViewModel.saveQueries()
        super.onPause()
    }
}
