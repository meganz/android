package mega.privacy.android.feature.clouddrive.presentation.folderlink

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.modifiers.excludingBottomPadding
import mega.privacy.android.core.nodecomponents.list.NodesView
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.list.rememberDynamicSpanCount
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkAction
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkContentState
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkUiState
import mega.privacy.android.navigation.contract.transition.fadeTransition
import mega.privacy.android.navigation.destination.TransfersNavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderLinkScreen(
    viewModel: FolderLinkViewModel,
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FolderLinkContent(
        uiState = uiState,
        onNavigate = onNavigate,
        onBack = onBack,
        onAction = viewModel::processAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderLinkContent(
    uiState: FolderLinkUiState,
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onAction: (FolderLinkAction) -> Unit,
) {
    val isListView = uiState.currentViewType == ViewType.LIST
    val spanCount = rememberDynamicSpanCount(isListView = isListView)
    var shouldShowSkeleton by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.contentState) {
        if (uiState.contentState is FolderLinkContentState.Loading) {
            delay(200L) // Show skeleton only for large folders
            if (this.isActive) {
                shouldShowSkeleton = true
            }
        } else {
            shouldShowSkeleton = false
        }
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(FOLDER_LINK_APP_BAR_TAG),
                title = uiState.title.text, // TODO update after finalized design
                navigationType = AppBarNavigationType.Back { onAction(FolderLinkAction.BackPressed) },
                trailingIcons = {
                    TransfersToolbarWidget {
                        onNavigate(TransfersNavKey())
                    }
                },
            )
        },
        bottomBar = {},
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding.excludingBottomPadding()),
        ) {
            when (val contentState = uiState.contentState) {
                FolderLinkContentState.Loading -> {
                    if (shouldShowSkeleton) {
                        NodesViewSkeleton(
                            isListView = isListView,
                            spanCount = spanCount,
                        )
                    }
                }

                is FolderLinkContentState.DecryptionKeyRequired -> {
                    // TODO: show decryption key dialog in later MR
                    MegaText(text = if (contentState.isKeyIncorrect) "Invalid decryption key" else "Decryption key required")
                }

                FolderLinkContentState.Expired ->
                    MegaText(text = "This link has expired")

                FolderLinkContentState.Unavailable ->
                    MegaText(text = "This link is unavailable")

                is FolderLinkContentState.Loaded -> {
                    AnimatedContent(
                        targetState = uiState.currentFolderNode?.id,
                        transitionSpec = { fadeTransition },
                        label = "folder_nav_fade",
                    ) { parentNodeId ->
                        key(parentNodeId) {
                            val listState = rememberLazyListState()
                            val gridState = rememberLazyGridState()
                            NodesView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                listContentPadding = PaddingValues(
                                    bottom = contentPadding.calculateBottomPadding() + 100.dp,
                                ),
                                listState = listState,
                                gridState = gridState,
                                spanCount = spanCount,
                                items = contentState.items,
                                isNextPageLoading = false,
                                isHiddenNodesEnabled = false,
                                showHiddenNodes = true,
                                onMenuClicked = {
                                    // TODO
                                },
                                onItemClicked = { onAction(FolderLinkAction.ItemClicked(it)) },
                                onLongClicked = {
                                    // TODO onAction(ItemLongClicked(it))
                                },
                                sortConfiguration = uiState.selectedSortConfiguration,
                                isListView = isListView,
                                onSortOrderClick = {
                                    // TODO
                                },
                                onChangeViewTypeClicked = {
                                    // TODO onAction(ChangeViewTypeClicked)
                                },
                                inSelectionMode = false, // TODO
                            )
                        }
                    }
                }
            }
        }
    }

    BackHandler { onAction(FolderLinkAction.BackPressed) }

    EventEffect(
        event = uiState.navigateBackEvent,
        onConsumed = { onAction(FolderLinkAction.NavigateBackEventConsumed) },
        action = onBack,
    )
}


internal const val FOLDER_LINK_APP_BAR_TAG = "folder_link_screen:main_app_bar"
