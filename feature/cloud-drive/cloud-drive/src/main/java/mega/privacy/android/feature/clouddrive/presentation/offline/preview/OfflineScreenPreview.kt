package mega.privacy.android.feature.clouddrive.presentation.offline.preview

import androidx.compose.runtime.Composable
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.clouddrive.presentation.offline.OfflineScreen
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineNodeUiItem
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineUiState

@CombinedThemePreviews
@Composable
private fun PreviewOfflineScreenList() {
    AndroidThemeForPreviews {
        OfflineScreen(
            uiState = OfflineUiState(
                isLoadingCurrentFolder = false,
                isLoadingChildFolders = false,
                offlineNodes = listOf(
                    getNodeUiItem(1),
                    getNodeUiItem(2),
                    getNodeUiItem(3),
                    getNodeUiItem(4),
                    getNodeUiItem(5)
                ),
                selectedNodeHandles = listOf(),
                nodeId = -1,
                title = "Offline",
                currentViewType = ViewType.LIST,
                isOnline = true,
                searchQuery = null,
                showOfflineWarning = true,
            ),
            selectAll = {},
            deselectAll = {},
            onItemClicked = {},
            onItemLongClicked = {},
            onBack = {},
            onNavigateToFolder = { _, _ -> },
            onOpenFile = {},
            onDismissOfflineWarning = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewOfflineScreenGrid() {
    AndroidThemeForPreviews {
        OfflineScreen(
            uiState = OfflineUiState(
                isLoadingCurrentFolder = false,
                isLoadingChildFolders = false,
                offlineNodes = listOf(
                    getNodeUiItem(1),
                    getNodeUiItem(2),
                    getNodeUiItem(3),
                    getNodeUiItem(4),
                    getNodeUiItem(5)
                ),
                selectedNodeHandles = listOf(),
                nodeId = -1,
                title = "Offline",
                currentViewType = ViewType.GRID,
                isOnline = true,
                searchQuery = null,
                showOfflineWarning = true,
            ),
            selectAll = {},
            deselectAll = {},
            onItemClicked = {},
            onItemLongClicked = {},
            onBack = {},
            onNavigateToFolder = { _, _ -> },
            onOpenFile = {},
            onDismissOfflineWarning = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewEmptyOfflineScreen() {
    AndroidThemeForPreviews {
        OfflineScreen(
            uiState = OfflineUiState(
                isLoadingCurrentFolder = false,
                isLoadingChildFolders = false,
                offlineNodes = listOf(),
                selectedNodeHandles = listOf(),
                nodeId = -1,
                title = "Offline",
                currentViewType = ViewType.LIST,
                isOnline = true,
                searchQuery = null,
                showOfflineWarning = true,
            ),
            selectAll = {},
            deselectAll = {},
            onItemClicked = {},
            onItemLongClicked = {},
            onBack = {},
            onNavigateToFolder = { _, _ -> },
            onOpenFile = {},
            onDismissOfflineWarning = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewLoadingOfflineScreen() {
    AndroidThemeForPreviews {
        OfflineScreen(
            uiState = OfflineUiState(
                isLoadingCurrentFolder = true,
                isLoadingChildFolders = false,
                offlineNodes = listOf(),
                selectedNodeHandles = listOf(),
                nodeId = -1,
                title = "Offline",
                currentViewType = ViewType.LIST,
                isOnline = true,
                searchQuery = null,
                showOfflineWarning = true,
            ),
            selectAll = {},
            deselectAll = {},
            onItemClicked = {},
            onItemLongClicked = {},
            onBack = {},
            onNavigateToFolder = { _, _ -> },
            onOpenFile = {},
            onDismissOfflineWarning = {}
        )
    }
}

private fun getNodeUiItem(id: Int) =
    OfflineNodeUiItem(
        offlineFileInformation = OfflineFileInformation(
            nodeInfo = OtherOfflineNodeInformation(
                id = id,
                path = "/Document-$id.pdf",
                parentId = 1,
                handle = "$id",
                name = "Document-$id.pdf",
                isFolder = false,
                lastModifiedTime = 0L
            ),
            totalSize = 1024L,
            absolutePath = "/offline/Document.pdf"
        ),
        isSelected = false,
        isHighlighted = false
    )
