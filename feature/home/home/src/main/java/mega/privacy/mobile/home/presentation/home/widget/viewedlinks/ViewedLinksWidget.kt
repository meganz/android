package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailUriRequest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.destination.FolderLinkNavKey
import mega.privacy.android.navigation.destination.LegacyFileLinkNavKey
import mega.privacy.android.navigation.destination.ViewedLinksScreenNavKey
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Home widget that displays recently viewed file and folder links.
 */
class ViewedLinksWidget @Inject constructor() : HomeWidget {

    override val identifier: String = "ViewedLinksWidget"
    override val defaultOrder: Int = 5
    override val canDelete: Boolean = true

    override suspend fun getWidgetName() =
        LocalizedText.StringRes(sharedR.string.home_widget_viewed_links_section_header)

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        navigationHandler: NavigationHandler,
        transferHandler: TransferHandler,
    ) {
        FeatureFlagGate(feature = ApiFeatures.ViewedLinks) {
            val viewModel: ViewedLinksViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val nodeOptionsActionViewModel =
                hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                    creationCallback = { it.create(NodeSourceType.FOLDER_LINK) }
                )

            HandleNodeOptionsActionResult(
                nodeOptionsActionViewModel = nodeOptionsActionViewModel,
                navigationHandler = navigationHandler,
                onTransfer = transferHandler::setTransferEvent,
            )

            ViewedLinksView(
                uiState = uiState,
                modifier = modifier,
                onFolderLinkClicked = { link ->
                    navigationHandler.navigate(FolderLinkNavKey(link))
                },
                onFileLinkClicked = { link ->
                    navigationHandler.navigate(LegacyFileLinkNavKey(link))
                },
                onViewAllClicked = {
                    navigationHandler.navigate(ViewedLinksScreenNavKey)
                },
                onMenuClicked = { item ->
                    navigationHandler.navigate(
                        NodeOptionsBottomSheetNavKey(
                            nodeHandle = item.viewedLink.nodeHandle,
                            nodeSourceType = NodeSourceType.FOLDER_LINK,
                            publicLinkUrl = item.viewedLink.linkUrl
                                .takeIf { item.viewedLink.type == RecentlyUsedType.FileLink },
                        )
                    )
                },
            )
        }
    }
}

@Composable
internal fun ViewedLinksView(
    uiState: ViewedLinksUiState,
    onFolderLinkClicked: (String) -> Unit,
    onFileLinkClicked: (String) -> Unit,
    onViewAllClicked: () -> Unit,
    onMenuClicked: (ViewedLinkUiItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> {
            // Todo: Add loading placeholder
        }

        uiState.items.isEmpty() -> {
            // Todo: Add empty layout
        }

        else -> {
            Column(modifier = modifier) {
                ViewedLinksWidgetHeader(onViewAllClicked = onViewAllClicked)

                uiState.items.take(4).forEach { item ->
                    OneLineListItem(
                        modifier = Modifier.fillMaxWidth(),
                        text = item.viewedLink.name,
                        leadingElement = {
                            NodeThumbnailView(
                                modifier = Modifier.size(32.dp),
                                layoutType = ThumbnailLayoutType.List,
                                data = item.previewPath?.let { ThumbnailUriRequest(UriPath(it)) },
                                defaultImage = item.iconRes,
                                contentDescription = "Thumbnail",
                            )
                        },
                        trailingElement = {
                            MegaIcon(
                                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical),
                                contentDescription = null,
                                modifier = Modifier
                                    .clickable { onMenuClicked(item) },
                                tint = IconColor.Primary,
                            )
                        },
                        onClickListener = {
                            when (item.viewedLink.type) {
                                RecentlyUsedType.FolderLink -> onFolderLinkClicked(item.viewedLink.linkUrl)
                                else -> onFileLinkClicked(item.viewedLink.linkUrl)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ViewedLinksWidgetHeader(
    onViewAllClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewAllClicked() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaText(
            text = stringResource(sharedR.string.home_widget_viewed_links_section_header),
            style = AppTheme.typography.titleMedium.copy(fontSize = 18.sp),
            modifier = Modifier
                .weight(1f)
                .testTag(VIEWED_LINKS_TITLE_TEST_TAG),
        )

        Box(
            modifier = Modifier
                .size(24.dp)
                .wrapContentSize(unbounded = true, align = Alignment.Center)
                .size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.ChevronRight,
                contentDescription = null,
                tint = IconColor.Secondary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

internal const val VIEWED_LINKS_TITLE_TEST_TAG = "viewed_links_widget:title"
