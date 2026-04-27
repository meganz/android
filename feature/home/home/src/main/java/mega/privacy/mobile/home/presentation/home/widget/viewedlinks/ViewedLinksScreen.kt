package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailUriRequest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Full-screen Viewed Links list. Displays all viewed file and folder links
 * without the 4-item limit used in the Home widget.
 *
 * @param uiState The UI state containing the list of viewed link items.
 * @param onFolderLinkClicked Callback when a folder link is tapped.
 * @param onFileLinkClicked Callback when a file link is tapped.
 * @param onBack Callback when the back button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ViewedLinksScreen(
    uiState: ViewedLinksUiState,
    onFolderLinkClicked: (String) -> Unit,
    onFileLinkClicked: (String) -> Unit,
    onBack: () -> Unit,
) {
    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.home_widget_viewed_links_section_header),
                navigationType = AppBarNavigationType.Back(onBack),
                actions = listOf(
                    MenuActionWithClick(CommonMenuAction.More) {
                        // Todo: Show bottom sheet to clear entries
                    }
                )
            )
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                // Todo: Add loading state
            }

            uiState.items.isEmpty() -> {
                // Todo: Add empty state
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    // Todo: Add header item

                    items(
                        items = uiState.items,
                        key = { it.viewedLink.nodeHandle },
                    ) { item ->
                        OneLineListItem(
                            modifier = Modifier.fillMaxWidth(),
                            text = item.viewedLink.name,
                            leadingElement = {
                                NodeThumbnailView(
                                    modifier = Modifier.size(32.dp),
                                    layoutType = ThumbnailLayoutType.List,
                                    data = item.previewPath?.let {
                                        ThumbnailUriRequest(UriPath(it))
                                    },
                                    defaultImage = item.iconRes,
                                    contentDescription = "Thumbnail",
                                )
                            },
                            trailingElement = {
                                MegaIcon(
                                    painter = rememberVectorPainter(
                                        IconPack.Medium.Thin.Outline.MoreVertical
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.clickable {
                                        // Todo: Open bottom sheet
                                    },
                                    tint = IconColor.Primary,
                                )
                            },
                            onClickListener = {
                                when (item.viewedLink.type) {
                                    RecentlyUsedType.FolderLink ->
                                        onFolderLinkClicked(item.viewedLink.linkUrl)

                                    else ->
                                        onFileLinkClicked(item.viewedLink.linkUrl)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ViewedLinksScreenPreview() {
    AndroidThemeForPreviews {
        ViewedLinksScreen(
            uiState = ViewedLinksUiState(
                isLoading = false,
                items = listOf(
                    ViewedLinkUiItem(
                        viewedLink = ViewedLink(
                            nodeHandle = 1L,
                            name = "Galicia 004.mov",
                            linkUrl = "https://mega.nz/file/abc",
                            type = RecentlyUsedType.FileLink,
                        ),
                        iconRes = iconPackR.drawable.ic_video_medium_solid,
                        previewPath = null,
                    ),
                    ViewedLinkUiItem(
                        viewedLink = ViewedLink(
                            nodeHandle = 2L,
                            name = "Galicia 005.mov",
                            linkUrl = "https://mega.nz/file/def",
                            type = RecentlyUsedType.FileLink,
                        ),
                        iconRes = iconPackR.drawable.ic_video_medium_solid,
                        previewPath = null,
                    ),
                    ViewedLinkUiItem(
                        viewedLink = ViewedLink(
                            nodeHandle = 3L,
                            name = "Susan Abulhawa notes.txt",
                            linkUrl = "https://mega.nz/file/ghi",
                            type = RecentlyUsedType.FileLink,
                        ),
                        iconRes = iconPackR.drawable.ic_text_medium_solid,
                        previewPath = null,
                    ),
                    ViewedLinkUiItem(
                        viewedLink = ViewedLink(
                            nodeHandle = 4L,
                            name = "Anne Carson - Gloves on article.pdf",
                            linkUrl = "https://mega.nz/file/jkl",
                            type = RecentlyUsedType.FileLink,
                        ),
                        iconRes = iconPackR.drawable.ic_pdf_medium_solid,
                        previewPath = null,
                    ),
                    ViewedLinkUiItem(
                        viewedLink = ViewedLink(
                            nodeHandle = 5L,
                            name = "Annemarie_Jacir",
                            linkUrl = "https://mega.nz/folder/mno",
                            type = RecentlyUsedType.FolderLink,
                        ),
                        iconRes = iconPackR.drawable.ic_folder_users_small_solid,
                        previewPath = null,
                    ),
                    ViewedLinkUiItem(
                        viewedLink = ViewedLink(
                            nodeHandle = 6L,
                            name = "Recipes",
                            linkUrl = "https://mega.nz/folder/pqr",
                            type = RecentlyUsedType.FolderLink,
                        ),
                        iconRes = iconPackR.drawable.ic_folder_users_small_solid,
                        previewPath = null,
                    ),
                    ViewedLinkUiItem(
                        viewedLink = ViewedLink(
                            nodeHandle = 7L,
                            name = "Nabulus_soap_company_products.pdf",
                            linkUrl = "https://mega.nz/file/stu",
                            type = RecentlyUsedType.FileLink,
                        ),
                        iconRes = iconPackR.drawable.ic_pdf_medium_solid,
                        previewPath = null,
                    ),
                ),
            ),
            onFolderLinkClicked = {},
            onFileLinkClicked = {},
            onBack = {},
        )
    }
}