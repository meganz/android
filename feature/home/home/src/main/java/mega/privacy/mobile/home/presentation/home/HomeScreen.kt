package mega.privacy.mobile.home.presentation.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.dialog.textfile.NewTextFileNodeDialog
import mega.privacy.android.core.nodecomponents.sheet.home.HomeFabOption
import mega.privacy.android.core.nodecomponents.sheet.home.HomeFabOptionsBottomSheetNavKey
import mega.privacy.android.core.nodecomponents.upload.ScanDocumentHandler
import mega.privacy.android.core.nodecomponents.upload.ScanDocumentViewModel
import mega.privacy.android.core.nodecomponents.upload.UploadingFiles
import mega.privacy.android.core.nodecomponents.upload.rememberCaptureHandler
import mega.privacy.android.core.nodecomponents.upload.rememberUploadHandler
import mega.privacy.android.core.sharedcomponents.extension.excludingBottomPadding
import mega.privacy.android.core.sharedcomponents.extension.systemBarsIgnoringBottom
import mega.privacy.android.core.sharedcomponents.menu.CommonAppBarAction
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.navigation.destination.SyncNewFolderNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.HomeFabOptionsButtonPressedEvent
import mega.privacy.mobile.home.presentation.home.model.HomeUiState
import mega.privacy.mobile.home.presentation.home.model.searchNavKey


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
    scanDocumentViewModel: ScanDocumentViewModel = hiltViewModel(),
) {
    val megaNavigator = rememberMegaNavigator()
    val megaResultContract = rememberMegaResultContract()
    val rootFolderId = NodeId(-1L)
    var uploadUris by rememberSaveable { mutableStateOf(emptyList<Uri>()) }
    var showNewTextFileDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = LocalSnackBarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val uploadHandler = rememberUploadHandler(
        parentId = rootFolderId,
        onFilesSelected = { uris ->
            uploadUris = uris
        },
        megaNavigator = megaNavigator,
        megaResultContract = megaResultContract
    )

    val captureHandler = rememberCaptureHandler(
        onPhotoCaptured = { uri ->
            uploadUris = listOf(uri)
        },
        megaResultContract = megaResultContract
    )

    val nameCollisionLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.nameCollisionActivityContract
    ) { message ->
        if (!message.isNullOrEmpty()) {
            coroutineScope.launch {
                snackbarHostState?.showAutoDurationSnackbar(message)
            }
        }
    }

    val fabOption by
    navigationHandler.monitorResult<HomeFabOption>(HomeFabOptionsBottomSheetNavKey.KEY)
        .collectAsStateWithLifecycle(null)

    LaunchedEffect(fabOption) {
        val fabOption = fabOption
        if (fabOption != null) {
            when (fabOption) {
                HomeFabOption.UploadFiles -> uploadHandler.onUploadFilesClicked()
                HomeFabOption.UploadFolder -> uploadHandler.onUploadFolderClicked()
                HomeFabOption.ScanDocument -> scanDocumentViewModel.prepareDocumentScanner()
                HomeFabOption.Capture -> captureHandler.onCaptureClicked()
                HomeFabOption.CreateNewTextFile -> showNewTextFileDialog = true
                HomeFabOption.AddNewSync -> navigationHandler.navigate(SyncNewFolderNavKey())
                HomeFabOption.AddNewBackup -> navigationHandler.navigate(
                    SyncNewFolderNavKey(
                        syncType = SyncType.TYPE_BACKUP
                    )
                )

                HomeFabOption.NewChat -> navigationHandler.navigate(
                    ChatListNavKey(createNewChat = true)
                )
            }
            navigationHandler.clearResult(HomeFabOptionsBottomSheetNavKey.KEY)
        }
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.general_section_home),
                navigationType = AppBarNavigationType.None,
                trailingIcons = { TransfersToolbarWidget(navigationHandler::navigate) },
                actions = buildList {
                    if (state is HomeUiState.Data) {
                        add(MenuActionWithClick(CommonAppBarAction.Search) {
                            navigationHandler.navigate(state.searchNavKey)
                        })
                    }
                }
            )
        },
        floatingActionButton = {
            if (state is HomeUiState.Data) {
                AddContentFab(
                    visible = true,
                    onClick = {
                        Analytics.tracker.trackEvent(HomeFabOptionsButtonPressedEvent)
                        navigationHandler.navigate(HomeFabOptionsBottomSheetNavKey)
                    }
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBarsIgnoringBottom,
    ) { paddingValues ->
        when (state) {
            is HomeUiState.Data -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues.excludingBottomPadding()),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(state.widgets, key = { it.identifier }) { it ->
                        it.content(Modifier, navigationHandler, transferHandler)
                    }
                }
            }

            is HomeUiState.Offline -> {
                HomeOfflineScreen(
                    hasOfflineFiles = state.hasOfflineFiles,
                    onViewOfflineFilesClick = {
                        navigationHandler.navigate(
                            mega.privacy.android.navigation.destination.OfflineNavKey()
                        )
                    },
                    modifier = Modifier.padding(paddingValues.excludingBottomPadding()),
                )
            }

            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    // Blank screen
                }
            }
        }
    }

    UploadingFiles(
        nameCollisionLauncher = nameCollisionLauncher,
        parentNodeId = rootFolderId,
        uris = uploadUris,
        onStartUpload = { transferTriggerEvent ->
            transferHandler.setTransferEvent(transferTriggerEvent)
            uploadUris = emptyList()
        },
    )

    ScanDocumentHandler(
        parentNodeId = rootFolderId,
        megaNavigator = megaNavigator,
        viewModel = scanDocumentViewModel
    )

    if (showNewTextFileDialog) {
        NewTextFileNodeDialog(
            parentNode = rootFolderId,
            onDismiss = {
                showNewTextFileDialog = false
            }
        )
    }
}
