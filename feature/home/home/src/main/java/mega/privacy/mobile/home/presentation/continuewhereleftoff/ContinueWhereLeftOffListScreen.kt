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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.indicators.LargeInfiniteSpinnerIndicator
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeSourceData
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.feature.home.R
import mega.privacy.android.navigation.contract.TransferHandler
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
