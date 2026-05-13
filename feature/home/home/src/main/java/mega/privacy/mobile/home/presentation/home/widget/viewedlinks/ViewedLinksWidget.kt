package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.navigation.Flagged
import mega.privacy.android.domain.entity.node.RecentlyViewedLinkType
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailUriRequest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.home.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.destination.FileLinkNavKey
import mega.privacy.android.navigation.destination.FolderLinkNavKey
import mega.privacy.android.navigation.destination.ViewedLinksScreenNavKey
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.home.widget.viewedlinks.ViewedLinksWidget.Companion.MAX_VISIBLE_VIEWED_LINK
import mega.privacy.mobile.home.presentation.home.widget.viewedlinks.view.ViewedLinkLoadingItem
import javax.inject.Inject

/**
 * Home widget that displays recently viewed file and folder links.
 */
class ViewedLinksWidget @Inject constructor() : HomeWidget, Flagged {
    override val identifier: String = "ViewedLinksWidget"
    override val defaultOrder: Int = 5
    override val canDelete: Boolean = true
    override val feature: Feature = ApiFeatures.ViewedLinks

    override suspend fun getWidgetName() =
        LocalizedText.StringRes(sharedR.string.home_widget_viewed_links_section_header)

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        navigationHandler: NavigationHandler,
        transferHandler: TransferHandler,
    ) {
        FeatureFlagGate(feature = feature) {
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
                    navigationHandler.navigate(FileLinkNavKey(link))
                },
                onViewAllClicked = {
                    navigationHandler.navigate(ViewedLinksScreenNavKey)
                },
            )
        }
    }

    companion object {
        const val MAX_VISIBLE_VIEWED_LINK = 4
    }
}

@Composable
internal fun ViewedLinksView(
    uiState: ViewedLinksUiState,
    onFolderLinkClicked: (String) -> Unit,
    onFileLinkClicked: (String) -> Unit,
    onViewAllClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ViewedLinksWidgetHeader(
            onViewAllClicked = onViewAllClicked,
            showMoreButton = uiState is ViewedLinksUiState.Ready
                    && uiState.items.size > MAX_VISIBLE_VIEWED_LINK,
        )

        when (uiState) {
            is ViewedLinksUiState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewAllClicked() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ViewedLinkLoadingItem()
                }
            }

            is ViewedLinksUiState.Ready -> {
                if (uiState.items.isEmpty()) {
                    ViewedLinksEmptyView()
                } else {
                    uiState.items.take(MAX_VISIBLE_VIEWED_LINK).forEach { item ->
                        OneLineListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(VIEWED_LINKS_ITEM_TEST_TAG),
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
                            onClickListener = {
                                when (item.viewedLink.type) {
                                    RecentlyViewedLinkType.FolderLink -> onFolderLinkClicked(item.viewedLink.linkUrl)
                                    RecentlyViewedLinkType.FileLink -> onFileLinkClicked(item.viewedLink.linkUrl)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ViewedLinksWidgetHeader(
    onViewAllClicked: () -> Unit,
    showMoreButton: Boolean = false,
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

        if (showMoreButton) {
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
}

@Composable
internal fun ViewedLinksEmptyView(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaText(
            text = stringResource(sharedR.string.home_widget_viewed_links_empty_state),
            style = AppTheme.typography.titleSmall,
            textColor = TextColor.Secondary,
            modifier = Modifier
                .weight(1f)
                .testTag(VIEWED_LINKS_EMPTY_TEXT_TEST_TAG),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            painter = painterResource(R.drawable.illustration_mega_secondary_link),
            contentDescription = null,
            modifier = Modifier.size(60.dp),
        )
    }
}

internal const val VIEWED_LINKS_TITLE_TEST_TAG = "viewed_links_widget:title"
internal const val VIEWED_LINKS_EMPTY_TEXT_TEST_TAG = "viewed_links_widget:empty_text"
