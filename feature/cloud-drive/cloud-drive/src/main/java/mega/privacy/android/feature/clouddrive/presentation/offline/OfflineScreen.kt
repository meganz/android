package mega.privacy.android.feature.clouddrive.presentation.offline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.banner.TopWarningBanner
import mega.android.core.ui.components.indicators.LargeHUD
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.nodecomponents.list.NodeGridViewItem
import mega.privacy.android.core.nodecomponents.list.NodeListViewItem
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.sharedcomponents.empty.MegaEmptyView
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineNodeUiItem
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineUiState
import mega.privacy.android.icon.pack.R as iconPackR

/**
 * OfflineScreen - A purely composable screen for displaying offline files
 *
 * @param onBack Callback for back navigation
 * @param viewModel The OfflineViewModel to manage state
 * @param modifier Modifier for the composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineScreen(
    onBack: () -> Unit,
    onNavigateToFolder: (nodeId: Int, name: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OfflineViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    OfflineScreen(
        uiState = uiState,
        onBack = onBack,
        onItemClicked = viewModel::onItemClicked,
        onItemLongClicked = viewModel::onLongItemClicked,
        onOpenFile = { node ->
            // Todo implement OfflineNodeActionsViewModel to handle open file action
        },
        onDismissOfflineWarning = viewModel::dismissOfflineWarning,
        onNavigateToFolder = onNavigateToFolder,
        consumeOpenFolderEvent = viewModel::onOpenFolderInPageEventConsumed,
        consumeOpenFileEvent = viewModel::onOpenOfflineNodeEventConsumed,
        modifier = modifier
    )
}

@Composable
internal fun OfflineScreen(
    uiState: OfflineUiState,
    onBack: () -> Unit,
    onItemClicked: (OfflineNodeUiItem) -> Unit,
    onItemLongClicked: (OfflineNodeUiItem) -> Unit,
    onNavigateToFolder: (nodeId: Int, name: String) -> Unit,
    onOpenFile: (String) -> Unit,
    onDismissOfflineWarning: () -> Unit,
    modifier: Modifier = Modifier,
    consumeOpenFolderEvent: () -> Unit = {},
    consumeOpenFileEvent: () -> Unit = {},
) {
    EventEffect(
        event = uiState.openFolderInPageEvent,
        onConsumed = consumeOpenFolderEvent
    ) { folderNode ->
        onNavigateToFolder(folderNode.id, folderNode.name)
    }

    EventEffect(
        event = uiState.openOfflineNodeEvent,
        onConsumed = consumeOpenFileEvent
    ) { file ->
        onOpenFile(file.name)
    }

    MegaScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MegaTopAppBar(
                title = uiState
                    .title
                    .takeIf { uiState.nodeId != -1 }
                    ?: stringResource(R.string.offline_screen_title),
                navigationType = AppBarNavigationType.Back(onBack)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (uiState.showOfflineWarning && !uiState.isLoading) {
                TopWarningBanner(
                    modifier = Modifier.fillMaxWidth(),
                    body = stringResource(R.string.offline_warning),
                    showCancelButton = true,
                    onCancelButtonClick = onDismissOfflineWarning
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        LargeHUD(
                            modifier = Modifier
                                .align(Alignment.Center)
                        )
                    }

                    uiState.offlineNodes.isEmpty() -> {
                        MegaEmptyView(
                            modifier = Modifier.align(Alignment.Center),
                            text = "No offline files available",
                            imagePainter = painterResource(iconPackR.drawable.ic_arrow_circle_down_glass)
                        )
                    }

                    else -> {
                        OfflineContent(
                            modifier = Modifier.fillMaxSize(),
                            uiState = uiState,
                            onItemClicked = onItemClicked,
                            onItemLongClicked = onItemLongClicked,
                            onMoreClicked = {
                                // Todo implement NodeOptionsBottomSheet
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineContent(
    uiState: OfflineUiState,
    onItemClicked: (OfflineNodeUiItem) -> Unit,
    onItemLongClicked: (OfflineNodeUiItem) -> Unit,
    onMoreClicked: (OfflineNodeUiItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fileTypeIconMapper = remember { FileTypeIconMapper() }

    when (uiState.currentViewType) {
        ViewType.LIST -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = uiState.offlineNodes,
                    key = { it.offlineFileInformation.handle }
                ) { node ->
                    NodeListViewItem(
                        title = node.offlineFileInformation.name,
                        subtitle = getOfflineNodeDescription(node.offlineFileInformation),
                        icon = if (node.offlineFileInformation.isFolder) {
                            iconPackR.drawable.ic_folder_medium_solid
                        } else {
                            val extension =
                                node.offlineFileInformation.name.substringAfterLast('.', "")
                                    .takeIf { it.isNotEmpty() }
                                    ?: return@items
                            fileTypeIconMapper(extension)
                        },
                        description = null,
                        tags = null,
                        thumbnailData = node.offlineFileInformation.thumbnail,
                        highlightText = uiState.searchQuery ?: "",
                        isSelected = node.isSelected,
                        isInSelectionMode = uiState.selectedNodeHandles.isNotEmpty(),
                        showIsVerified = false,
                        isTakenDown = false,
                        label = null,
                        showLink = false,
                        showFavourite = false,
                        isSensitive = false,
                        showBlurEffect = false,
                        isHighlighted = node.isHighlighted,
                        onMoreClicked = { onMoreClicked(node) },
                        onItemClicked = { onItemClicked(node) },
                        onLongClicked = { onItemLongClicked(node) }
                    )
                }
            }
        }

        ViewType.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                modifier = modifier,
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.offlineNodes,
                    key = { it.offlineFileInformation.handle }
                ) { node ->
                    NodeGridViewItem(
                        name = node.offlineFileInformation.name,
                        iconRes = if (node.offlineFileInformation.isFolder) {
                            iconPackR.drawable.ic_folder_medium_solid
                        } else {
                            val extension =
                                node.offlineFileInformation.name.substringAfterLast('.', "")
                                    .takeIf { it.isNotEmpty() }
                                    ?: return@items
                            fileTypeIconMapper(extension)
                        },
                        thumbnailData = node.offlineFileInformation.thumbnail,
                        isTakenDown = false,
                        duration = null,
                        isSelected = node.isSelected,
                        isInSelectionMode = uiState.selectedNodeHandles.isNotEmpty(),
                        isFolderNode = node.offlineFileInformation.isFolder,
                        isVideoNode = false, // TODO: Add video detection
                        highlightText = uiState.searchQuery ?: "",
                        isSensitive = false,
                        showBlurEffect = false,
                        isHighlighted = node.isHighlighted,
                        showLink = false,
                        showFavourite = false,
                        label = null,
                        onClick = { onItemClicked(node) },
                        onLongClick = { onItemLongClicked(node) },
                        onMenuClick = { onMoreClicked(node) }
                    )
                }
            }
        }
    }
}

@Composable
private fun getOfflineNodeDescription(offlineFileInformation: OfflineFileInformation): String {
    val context = LocalContext.current
    return if (offlineFileInformation.isFolder) {
        offlineFileInformation.folderInfo?.let { folderInfo ->
            if (folderInfo.numFolders == 0 && folderInfo.numFiles == 0) {
                stringResource(R.string.file_browser_empty_folder)
            } else if (folderInfo.numFolders == 0 && folderInfo.numFiles > 0) {
                pluralStringResource(
                    R.plurals.num_files_with_parameter,
                    folderInfo.numFiles,
                    folderInfo.numFiles
                )
            } else if (folderInfo.numFiles == 0 && folderInfo.numFolders > 0) {
                pluralStringResource(
                    R.plurals.num_folders_with_parameter,
                    folderInfo.numFolders,
                    folderInfo.numFolders
                )
            } else {
                pluralStringResource(
                    R.plurals.num_folders_num_files,
                    folderInfo.numFolders,
                    folderInfo.numFolders
                ) + pluralStringResource(
                    R.plurals.num_folders_num_files_2,
                    folderInfo.numFiles,
                    folderInfo.numFiles
                )
            }
        } ?: run {
            ""
        }
    } else {
        formatFileSize(offlineFileInformation.totalSize, context)
            .plus(
                offlineFileInformation
                    .addedTime
                    ?.let {
                        " · ".plus(
                            formatModifiedDate(
                                java.util.Locale(
                                    Locale.current.language, Locale.current.region
                                ),
                                it
                            )
                        )
                    } ?: "")
    }
}
