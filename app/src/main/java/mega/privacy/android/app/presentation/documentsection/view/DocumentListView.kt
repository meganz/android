package mega.privacy.android.app.presentation.documentsection.view

import mega.privacy.android.core.R as coreR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntity
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.icon.pack.R
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.legacy.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.theme.MegaAppTheme
import nz.mega.sdk.MegaNode

@Composable
internal fun DocumentListView(
    items: List<DocumentUiEntity>,
    lazyListState: LazyListState,
    sortOrder: String,
    modifier: Modifier,
    onChangeViewTypeClick: () -> Unit,
    onClick: (item: DocumentUiEntity, index: Int) -> Unit,
    onMenuClick: (DocumentUiEntity) -> Unit,
    onSortOrderClick: () -> Unit,
    onLongClick: ((item: DocumentUiEntity, index: Int) -> Unit) = { _, _ -> },
) {
    LazyColumn(state = lazyListState, modifier = modifier) {
        item(
            key = "header"
        ) {
            HeaderViewItem(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                onEnterMediaDiscoveryClick = {},
                sortOrder = sortOrder,
                isListView = true,
                showSortOrder = true,
                showChangeViewType = true,
                showMediaDiscoveryButton = false,
            )
        }

        items(count = items.size, key = { items[it].id.longValue }) {
            val documentItem = items[it]
            NodeListViewItem(
                modifier = Modifier.testTag("$DOCUMENT_SECTION_ITEM_VIEW_TEST_TAG$it"),
                isSelected = documentItem.isSelected,
                folderInfo = null,
                icon = documentItem.icon,
                infoIcon = if (documentItem.hasVersions) coreR.drawable.ic_version_small else null,
                fileSize = formatFileSize(documentItem.size, LocalContext.current),
                modifiedDate = formatModifiedDate(
                    java.util.Locale(
                        Locale.current.language, Locale.current.region
                    ),
                    documentItem.modificationTime
                ),
                name = documentItem.name,
                isTakenDown = documentItem.isTakenDown,
                showMenuButton = true,
                thumbnailData = if (documentItem.thumbnail?.exists() == true) {
                    documentItem.thumbnail
                } else {
                    ThumbnailRequest(documentItem.id)
                },
                isFavourite = documentItem.isFavourite,
                isSharedWithPublicLink = documentItem.isExported,
                labelColor = if (documentItem.label != MegaNode.NODE_LBL_UNKNOWN)
                    colorResource(
                        id = MegaNodeUtil.getNodeLabelColor(
                            documentItem.label
                        )
                    ) else null,
                nodeAvailableOffline = documentItem.nodeAvailableOffline,
                onClick = { onClick(documentItem, it) },
                onLongClick = { onLongClick(documentItem, it) },
                onMenuClick = { onMenuClick(documentItem) },
            )
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 72.dp),
                color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                thickness = 1.dp
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun DocumentListViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DocumentListView(
            items = getPreviewItems(),
            lazyListState = rememberLazyListState(),
            sortOrder = "Size",
            modifier = Modifier,
            onChangeViewTypeClick = {},
            onClick = { _, _ -> },
            onMenuClick = {},
            onSortOrderClick = {},
            onLongClick = { _, _ -> },
        )
    }
}

private fun getPreviewItems() = listOf(
    DocumentUiEntity(
        id = NodeId(1),
        name = "Document 1.txt",
        size = 1000,
        modificationTime = 100000,
        label = MegaNode.NODE_LBL_UNKNOWN,
        icon = R.drawable.ic_pdf_medium_solid,
        fileTypeInfo = PdfFileTypeInfo
    ),
    DocumentUiEntity(
        id = NodeId(2),
        name = "Document 2.pdf",
        size = 2000,
        modificationTime = 200000,
        label = MegaNode.NODE_LBL_GREEN,
        isFavourite = true,
        isExported = true,
        hasVersions = true,
        nodeAvailableOffline = true,
        icon = R.drawable.ic_text_medium_solid,
        fileTypeInfo = TextFileTypeInfo(
            mimeType = "text/plain",
            extension = "txt"
        )
    ),
    DocumentUiEntity(
        id = NodeId(3),
        name = "Document 3.docx",
        size = 3000,
        modificationTime = 300000,
        label = MegaNode.NODE_LBL_RED,
        icon = R.drawable.ic_word_medium_solid,
        fileTypeInfo = TextFileTypeInfo(
            mimeType = "text/plain",
            extension = "docx"
        )
    ),
)

/**
 * Test tag for the item view.
 */
const val DOCUMENT_SECTION_ITEM_VIEW_TEST_TAG = "document_section_list:item_view"