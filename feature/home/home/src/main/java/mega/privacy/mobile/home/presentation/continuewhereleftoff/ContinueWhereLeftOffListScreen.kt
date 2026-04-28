package mega.privacy.mobile.home.presentation.continuewhereleftoff

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.indicators.LargeInfiniteSpinnerIndicator
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeSourceData
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.feature.home.R
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.nodes.components.NodeHeaderItem
import mega.privacy.android.shared.nodes.components.SortBottomSheet
import mega.privacy.android.shared.nodes.components.SortBottomSheetResult
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContinueWhereLeftOffListScreen(
    viewModel: ContinueWhereLeftOffListViewModel,
    onNavigate: (NavKey) -> Unit,
    transferHandler: TransferHandler,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var openedFileNode by remember { mutableStateOf<TypedFileNode?>(null) }
    val sortSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    EventEffect(
        event = uiState.openNodeEvent,
        onConsumed = viewModel::onOpenNodeEventConsumed,
    ) { node ->
        openedFileNode = node
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.home_widget_continue_where_left_off),
                navigationType = AppBarNavigationType.Back { onBack() },
                actions = listOf(
                    MenuActionWithClick(CommonMenuAction.More) {
                        viewModel.showOptionsSheet()
                    }
                ),
            )
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    LargeInfiniteSpinnerIndicator()
                }
            }

            uiState.items.isEmpty() -> {
                EmptyStateView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    illustration = R.drawable.ic_cwlo_empty_state,
                    description = SpannableText(
                        text = stringResource(sharedR.string.home_cwlo_empty_state),
                    ),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    item(key = "sort_header") {
                        NodeHeaderItem(
                            onSortOrderClick = viewModel::showSortSheet,
                            onChangeViewTypeClick = {},
                            sortConfiguration = uiState.sortConfiguration,
                            isListView = true,
                            showSortOrder = true,
                            showChangeViewType = false,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp),
                        )
                    }
                    items(uiState.items, key = { it.nodeHandle }) { item ->
                        ContinueWhereLeftOffListItem(
                            title = item.title,
                            icon = iconForType(item.type),
                            onItemClicked = { viewModel.onItemClicked(item.nodeHandle) },
                            onMenuClicked = {
                                onNavigate(
                                    NodeOptionsBottomSheetNavKey(
                                        nodeHandle = item.nodeHandle,
                                        nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                                    )
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    if (uiState.showSortSheet) {
        SortBottomSheet(
            title = stringResource(sharedR.string.action_sort_by_header),
            options = NodeSortOption.getOptionsForSourceType(NodeSourceType.CLOUD_DRIVE),
            sheetState = sortSheetState,
            selectedSort = SortBottomSheetResult(
                sortOptionItem = uiState.sortConfiguration.sortOption,
                sortDirection = uiState.sortConfiguration.sortDirection,
            ),
            onDismissRequest = viewModel::dismissSortSheet,
            onSortOptionSelected = { result ->
                if (result != null) {
                    viewModel.updateSortConfiguration(
                        NodeSortConfiguration(
                            sortOption = result.sortOptionItem,
                            sortDirection = result.sortDirection,
                        )
                    )
                } else {
                    viewModel.dismissSortSheet()
                }
            },
        )
    }

    if (uiState.showOptionsSheet) {
        MegaModalBottomSheet(
            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
            sheetState = optionsSheetState,
            onDismissRequest = viewModel::dismissOptionsSheet,
        ) {
            NodeActionListTile(
                text = stringResource(sharedR.string.home_cwlo_clear_history),
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Eraser),
                onActionClicked = viewModel::clearAll,
                modifier = Modifier.testTag(CLEAR_HISTORY_TAG),
            )
        }
    }

    openedFileNode?.let { node ->
        HandleNodeAction3(
            typedFileNode = node,
            onActionHandled = { openedFileNode = null },
            nodeSourceData = NodeSourceData.Default(NodeSourceType.CLOUD_DRIVE),
            onDownloadEvent = transferHandler::setTransferEvent,
            onNavigate = onNavigate,
        )
    }
}

@Composable
private fun ContinueWhereLeftOffListItem(
    title: String,
    @DrawableRes icon: Int,
    onItemClicked: () -> Unit,
    onMenuClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClicked() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp)),
            painter = painterResource(icon),
            contentDescription = title,
        )
        MegaText(
            text = title,
            textColor = TextColor.Primary,
            style = AppTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .wrapContentSize(unbounded = true, align = Alignment.Center)
                .size(48.dp)
                .clickable { onMenuClicked() },
            contentAlignment = Alignment.Center,
        ) {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.MoreVertical,
                contentDescription = null,
                tint = IconColor.Secondary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

internal const val CLEAR_HISTORY_TAG = "cwlo_list:clear_history"

@CombinedThemePreviews
@Composable
private fun ContinueWhereLeftOffListItemPreview() {
    AndroidThemeForPreviews {
        Column {
            ContinueWhereLeftOffListItem(
                title = "Falastin36_press_trailer.mov",
                icon = IconPackR.drawable.ic_video_medium_solid,
                onItemClicked = {},
                onMenuClicked = {},
            )
            ContinueWhereLeftOffListItem(
                title = "Interview Agnes Varda.pdf",
                icon = IconPackR.drawable.ic_pdf_medium_solid,
                onItemClicked = {},
                onMenuClicked = {},
            )
            ContinueWhereLeftOffListItem(
                title = "Annemarie_Jacir_notes.txt",
                icon = IconPackR.drawable.ic_text_medium_solid,
                onItemClicked = {},
                onMenuClicked = {},
            )
        }
    }
}
