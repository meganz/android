package mega.privacy.mobile.home.presentation.recents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.sheet.home.HomeFabOptionsBottomSheetNavKey
import mega.privacy.android.core.sharedcomponents.menu.CommonAppBarAction
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.feature.home.R
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.RecentsBucketScreenNavKey
import mega.privacy.mobile.home.presentation.home.widget.recents.mockRecentsUiItemList
import mega.privacy.mobile.home.presentation.recents.model.RecentsUiItem
import mega.privacy.mobile.home.presentation.recents.model.RecentsUiState
import mega.privacy.mobile.home.presentation.recents.view.RecentsEmptyView
import mega.privacy.mobile.home.presentation.recents.view.RecentsHiddenView
import mega.privacy.mobile.home.presentation.recents.view.RecentsLazyListView
import mega.privacy.mobile.home.presentation.recents.view.RecentsLoadingView
import mega.privacy.mobile.home.presentation.recents.view.RecentsOptionsBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    viewModel: RecentsViewModel,
    onNavigate: (NavKey) -> Unit,
    transferHandler: TransferHandler,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackBarEventQueue = rememberSnackBarQueue()
    val snackBarHostState = LocalSnackBarHostState.current
    var openedFileNode by remember { mutableStateOf<Pair<TypedFileNode, NodeSourceType>?>(null) }
    var showOptionsBottomSheet by rememberSaveable { mutableStateOf(false) }

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                title = stringResource(R.string.section_recents),
                navigationType = AppBarNavigationType.Back(onBack),
                actions = listOf(
                    MenuActionWithClick(CommonAppBarAction.More) {
                        showOptionsBottomSheet = true
                    }
                )
            )
        },
    ) { paddingValues ->
        RecentsScreenContent(
            uiState = uiState,
            modifier = Modifier.padding(paddingValues),
            onFileClicked = { node, source ->
                openedFileNode = node to source
            },
            onBucketClicked = { item ->
                onNavigate(
                    RecentsBucketScreenNavKey(
                        identifier = item.bucket.identifier,
                        isMediaBucket = item.isMediaBucket,
                        folderName = item.bucket.parentFolderName,
                        nodeSourceType = item.nodeSourceType,
                        timestamp = item.bucket.timestamp,
                        fileCount = item.bucket.nodes.size,
                    )
                )
            },
            onShowRecentActivity = viewModel::showRecentActivity,
            onUploadClicked = {
                onNavigate(HomeFabOptionsBottomSheetNavKey)
            },
        )
    }

    openedFileNode?.let { (node, source) ->
        HandleNodeAction3(
            typedFileNode = node,
            snackBarHostState = snackBarHostState,
            coroutineScope = coroutineScope,
            onActionHandled = { openedFileNode = null },
            nodeSourceType = source,
            onDownloadEvent = transferHandler::setTransferEvent,
            onNavigate = onNavigate,
        )
    }

    RecentsOptionsBottomSheet(
        isVisible = showOptionsBottomSheet,
        onDismiss = { showOptionsBottomSheet = false },
        isHideRecentsEnabled = uiState.isHideRecentsEnabled,
        onShowRecentActivity = viewModel::showRecentActivity,
        onHideRecentActivity = {
            viewModel.hideRecentActivity()
            coroutineScope.launch {
                snackBarEventQueue.queueMessage("Your recent activity has been hidden") // TODO: Localize
            }
        },
    )
}

@Composable
internal fun RecentsScreenContent(
    uiState: RecentsUiState,
    onFileClicked: (TypedFileNode, NodeSourceType) -> Unit,
    onBucketClicked: (RecentsUiItem) -> Unit,
    onShowRecentActivity: () -> Unit,
    onUploadClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isHideRecentsEnabled -> {
            Box(modifier = modifier) {
                RecentsHiddenView(
                    onShowActivityClicked = onShowRecentActivity,
                )
            }
        }

        uiState.isLoading -> {
            Box(modifier = modifier) {
                RecentsLoadingView()
            }
        }

        uiState.isEmpty -> {
            Box(modifier = modifier) {
                RecentsEmptyView(
                    onUploadClicked = onUploadClicked
                )
            }
        }

        else -> {
            RecentsLazyListView(
                items = uiState.recentActionItems,
                modifier = modifier,
                onFileClicked = onFileClicked,
                onBucketClicked = onBucketClicked,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun RecentsScreenContentPreview() {
    AndroidThemeForPreviews {
        RecentsScreenContent(
            uiState = RecentsUiState(
                recentActionItems = mockRecentsUiItemList(),
                isNodesLoading = false,
                isHiddenNodeSettingsLoading = false
            ),
            onFileClicked = { _, _ -> },
            onBucketClicked = { },
            onShowRecentActivity = {},
            onUploadClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RecentsScreenContentEmptyPreview() {
    AndroidThemeForPreviews {
        RecentsScreenContent(
            uiState = RecentsUiState(
                recentActionItems = emptyList(),
                isNodesLoading = false,
                isHiddenNodeSettingsLoading = false
            ),
            onFileClicked = { _, _ -> },
            onBucketClicked = { },
            onShowRecentActivity = {},
            onUploadClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RecentsScreenContentLoadingPreview() {
    AndroidThemeForPreviews {
        RecentsScreenContent(
            uiState = RecentsUiState(
                recentActionItems = emptyList(),
                isNodesLoading = true,
                isHiddenNodeSettingsLoading = false
            ),
            onFileClicked = { _, _ -> },
            onBucketClicked = { },
            onShowRecentActivity = {},
            onUploadClicked = {},
        )
    }
}