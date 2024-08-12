package mega.privacy.android.app.presentation.documentsection.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntity
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.icon.pack.R
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import nz.mega.sdk.MegaNode

@Composable
internal fun DocumentListView(
    items: List<DocumentUiEntity>,
    accountType: AccountType?,
    lazyListState: LazyListState,
    sortOrder: String,
    modifier: Modifier,
    onChangeViewTypeClick: () -> Unit,
    onClick: (item: DocumentUiEntity, index: Int) -> Unit,
    onMenuClick: (DocumentUiEntity) -> Unit,
    onSortOrderClick: () -> Unit,
    isSelectionMode: Boolean,
    onLongClick: (item: DocumentUiEntity, index: Int) -> Unit = { _, _ -> },
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
                modifier = Modifier
                    .testTag("$DOCUMENT_SECTION_ITEM_VIEW_TEST_TAG$it")
                    .alpha(0.5f.takeIf {
                        accountType?.isPaid == true && (documentItem.isMarkedSensitive || documentItem.isSensitiveInherited)
                    } ?: 1f),
                isSensitive = accountType?.isPaid == true && (documentItem.isMarkedSensitive || documentItem.isSensitiveInherited),
                showBlurEffect = true,
                isSelected = documentItem.isSelected,
                icon = documentItem.icon,
                showVersion = documentItem.hasVersions,
                subtitle = formatFileSize(documentItem.size, LocalContext.current).plus(" • ")
                    .plus(
                        formatModifiedDate(
                            java.util.Locale(
                                Locale.current.language, Locale.current.region
                            ),
                            documentItem.modificationTime
                        )
                    ),
                title = documentItem.name,
                isTakenDown = documentItem.isTakenDown,
                thumbnailData = if (documentItem.thumbnail?.exists() == true) {
                    documentItem.thumbnail
                } else {
                    ThumbnailRequest(documentItem.id)
                },
                showFavourite = documentItem.isFavourite,
                showLink = documentItem.isExported,
                labelColor = if (documentItem.label != MegaNode.NODE_LBL_UNKNOWN)
                    colorResource(
                        id = MegaNodeUtil.getNodeLabelColor(
                            documentItem.label
                        )
                    ) else null,
                showOffline = documentItem.nodeAvailableOffline,
                onItemClicked = { onClick(documentItem, it) },
                onLongClick = { onLongClick(documentItem, it) },
                onMoreClicked = { onMenuClick(documentItem) }.takeIf { isSelectionMode.not() },
            )
            MegaDivider(dividerType = DividerType.BigStartPadding)
        }
    }
}

@CombinedThemePreviews
@Composable
private fun DocumentListViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        DocumentListView(
            items = getPreviewItems(),
            accountType = AccountType.FREE,
            lazyListState = rememberLazyListState(),
            sortOrder = "Size",
            modifier = Modifier,
            onChangeViewTypeClick = {},
            onClick = { _, _ -> },
            onMenuClick = {},
            onSortOrderClick = {},
            onLongClick = { _, _ -> },
            isSelectionMode = true,
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