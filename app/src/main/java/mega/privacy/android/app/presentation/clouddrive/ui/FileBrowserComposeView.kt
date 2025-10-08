@file:OptIn(ExperimentalComposeUiApi::class)

package mega.privacy.android.app.presentation.clouddrive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.clouddrive.model.FileBrowserState
import mega.privacy.android.core.nodecomponents.components.banners.StorageOverQuotaCapacity
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.search.view.LoadingStateView
import mega.privacy.android.app.presentation.view.NODES_EMPTY_VIEW_VISIBLE
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.app.presentation.view.OverQuotaView
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView
import mega.privacy.android.shared.original.core.ui.controls.banners.WarningBanner
import mega.privacy.android.shared.original.core.ui.model.rememberListGridNavigationState

/**
 * Composable view for FileBrowser
 * @param uiState
 * @param emptyState
 * @param onItemClick
 * @param onLongClick
 * @param onMenuClick
 * @param sortOrder
 * @param onSortOrderClick
 * @param onChangeViewTypeClick
 * @param onDisputeTakeDownClicked
 * @param onLinkClicked
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FileBrowserComposeView(
    uiState: FileBrowserState,
    emptyState: Pair<Int, Int>,
    onItemClick: (NodeUIItem<TypedNode>) -> Unit,
    onLongClick: (NodeUIItem<TypedNode>) -> Unit,
    onMenuClick: (NodeUIItem<TypedNode>) -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onLinkClicked: (String) -> Unit,
    onDisputeTakeDownClicked: (String) -> Unit,
    onUpgradeClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    onEnterMediaDiscoveryClick: () -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    onStorageAlmostFullWarningDismiss: () -> Unit,
) {

    val currentListState = rememberListGridNavigationState(
        currentHandle = uiState.fileBrowserHandle,
        navigationHandles = uiState.openedFolderNodeHandles
    )

    Column(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        uiState.errorMessage?.let { errorMessage ->
            WarningBanner(textString = stringResource(id = errorMessage), onCloseClick = null)
        }

        if (uiState.storageCapacity != StorageOverQuotaCapacity.DEFAULT) {
            StorageOverQuotaBanner(
                storageCapacity = uiState.storageCapacity,
                onStorageAlmostFullWarningDismiss = onStorageAlmostFullWarningDismiss,
                onUpgradeClicked = onUpgradeClicked
            )
        }

        if (!uiState.isLoading) {
            if (uiState.nodesList.isNotEmpty()) {
                Column {
                    OverQuotaView(
                        bannerTime = uiState.bannerTime,
                        shouldShowBannerVisibility = uiState.shouldShowBannerVisibility,
                        onUpgradeClicked = onUpgradeClicked,
                        onDismissClicked = onDismissClicked
                    )

                    NodesView(
                        nodeUIItems = uiState.nodesList,
                        onMenuClick = onMenuClick,
                        onItemClicked = onItemClick,
                        onLongClick = onLongClick,
                        sortOrder = sortOrder,
                        isListView = uiState.currentViewType == ViewType.LIST,
                        onSortOrderClick = onSortOrderClick,
                        onChangeViewTypeClick = onChangeViewTypeClick,
                        listState = currentListState.lazyListState,
                        gridState = currentListState.lazyGridState,
                        onLinkClicked = onLinkClicked,
                        onDisputeTakeDownClicked = onDisputeTakeDownClicked,
                        showMediaDiscoveryButton = uiState.showMediaDiscoveryIcon,
                        onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
                        listContentPadding = PaddingValues(top = 18.dp, bottom = 150.dp),
                        fileTypeIconMapper = fileTypeIconMapper,
                        inSelectionMode = uiState.isInSelection,
                        shouldApplySensitiveMode = uiState.hiddenNodeEnabled
                                && uiState.accountType?.isPaid == true
                                && !uiState.isBusinessAccountExpired,
                    )
                }
            } else {
                LegacyMegaEmptyView(
                    modifier = Modifier.testTag(NODES_EMPTY_VIEW_VISIBLE),
                    imagePainter = painterResource(id = emptyState.first),
                    text = stringResource(id = emptyState.second)
                )
            }
        } else if (uiState.hasNoOpenedFolders) {
            LoadingStateView(
                isList = uiState.currentViewType == ViewType.LIST
            )
        }
    }
}
