package mega.privacy.android.app.presentation.documentsection.view

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.documentsection.model.DocumentSectionUiState
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntity
import mega.privacy.android.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.white_black
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView
import mega.privacy.android.shared.theme.MegaAppTheme
import nz.mega.sdk.MegaNode

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DocumentSectionComposeView(
    modifier: Modifier,
    uiState: DocumentSectionUiState,
    onChangeViewTypeClick: () -> Unit,
    onClick: (item: DocumentUiEntity, index: Int) -> Unit,
    onSortOrderClick: () -> Unit,
    onMenuClick: (DocumentUiEntity) -> Unit,
    onLongClick: (item: DocumentUiEntity, index: Int) -> Unit,
    onAddDocumentClick: () -> Unit,
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val progressBarShowing = uiState.isLoading
    val items = uiState.allDocuments
    val scrollToTop = uiState.scrollToTop

    LaunchedEffect(items) {
        if (scrollToTop) {
            if (uiState.currentViewType == ViewType.LIST)
                listState.scrollToItem(0)
            else
                gridState.scrollToItem(0)
        }
    }

    MegaScaffold(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        scaffoldState = rememberScaffoldState(),
        floatingActionButton = {
            val scrollNotInProgress by remember {
                derivedStateOf {
                    if (uiState.currentViewType == ViewType.LIST) {
                        !listState.isScrollInProgress
                    } else {
                        !gridState.isScrollInProgress
                    }
                }
            }
            AddDocumentFabButton(
                showFabButton = scrollNotInProgress,
                onAddDocumentClick = onAddDocumentClick
            )
        }
    ) { paddingValue ->
        Box(modifier = modifier.padding(paddingValue)) {
            when {
                progressBarShowing -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 20.dp)
                            .testTag(DOCUMENT_SECTION_PROGRESS_BAR_TEST_TAG),
                        contentAlignment = Alignment.TopCenter,
                        content = {
                            MegaCircularProgressIndicator(
                                modifier = Modifier
                                    .size(50.dp),
                                strokeWidth = 4.dp,
                            )
                        },
                    )
                }

                items.isEmpty() -> LegacyMegaEmptyView(
                    modifier = Modifier.testTag(DOCUMENT_SECTION_EMPTY_VIEW_TEST_TAG),
                    text = stringResource(id = R.string.homepage_empty_hint_documents),
                    imagePainter = painterResource(id = R.drawable.ic_homepage_empty_document)
                )

                else -> {
                    val sortOrder = stringResource(
                        id = SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                            ?: R.string.sortby_name
                    )
                    if (uiState.currentViewType == ViewType.LIST) {
                        DocumentListView(
                            items = items,
                            lazyListState = listState,
                            sortOrder = sortOrder,
                            modifier = modifier.testTag(DOCUMENT_SECTION_LIST_VIEW_TEST_TAG),
                            onChangeViewTypeClick = onChangeViewTypeClick,
                            onClick = onClick,
                            onMenuClick = onMenuClick,
                            onSortOrderClick = onSortOrderClick,
                            onLongClick = onLongClick,
                            isSelectionMode = uiState.selectedDocumentHandles.isNotEmpty(),
                        )
                    } else {
                        DocumentGridView(
                            items = items,
                            lazyGridState = gridState,
                            sortOrder = sortOrder,
                            modifier = modifier.testTag(DOCUMENT_SECTION_GRID_VIEW_TEST_TAG),
                            onChangeViewTypeClick = onChangeViewTypeClick,
                            onClick = onClick,
                            onMenuClick = onMenuClick,
                            onSortOrderClick = onSortOrderClick,
                            onLongClick = onLongClick,
                            isSelectionMode = uiState.selectedDocumentHandles.isNotEmpty(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AddDocumentFabButton(
    onAddDocumentClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFabButton: Boolean = true,
) {
    AnimatedVisibility(
        visible = showFabButton,
        enter = scaleIn(),
        exit = scaleOut(),
        modifier = modifier
    ) {
        FloatingActionButton(
            modifier = modifier.testTag(DOCUMENT_SECTION_FAB_BUTTON_TEST_TAG),
            onClick = onAddDocumentClick
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create new video playlist",
                tint = MaterialTheme.colors.white_black
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun DocumentSectionComposeViewWithoutDocumentsPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DocumentSectionComposeView(
            uiState = DocumentSectionUiState(
                isLoading = false
            ),
            modifier = Modifier,
            onChangeViewTypeClick = {},
            onClick = { _, _ -> },
            onMenuClick = {},
            onSortOrderClick = {},
            onLongClick = { _, _ -> },
            onAddDocumentClick = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DocumentSectionComposeViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DocumentSectionComposeView(
            uiState = DocumentSectionUiState(
                allDocuments = getPreviewItems(),
                isLoading = false
            ),
            modifier = Modifier,
            onChangeViewTypeClick = {},
            onClick = { _, _ -> },
            onMenuClick = {},
            onSortOrderClick = {},
            onLongClick = { _, _ -> },
            onAddDocumentClick = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun AddDocumentFabButtonPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        AddDocumentFabButton(
            onAddDocumentClick = {},
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
        icon = iconPackR.drawable.ic_pdf_medium_solid,
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
        icon = iconPackR.drawable.ic_text_medium_solid,
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
        icon = iconPackR.drawable.ic_word_medium_solid,
        fileTypeInfo = TextFileTypeInfo(
            mimeType = "text/plain",
            extension = "docx"
        )
    ),
)

/**
 * Test tag for adding document fab button
 */
const val DOCUMENT_SECTION_FAB_BUTTON_TEST_TAG = "document_section:button_add_document"

/**
 * Test tag for the progress bar view.
 */
const val DOCUMENT_SECTION_PROGRESS_BAR_TEST_TAG = "document_section:progress_bar_loading"

/**
 * Test tag for the empty view.
 */
const val DOCUMENT_SECTION_EMPTY_VIEW_TEST_TAG = "document_section:empty_view"

/**
 * Test tag for the list view.
 */
const val DOCUMENT_SECTION_LIST_VIEW_TEST_TAG = "document_section:list_view"

/**
 * Test tag for the grid view.
 */
const val DOCUMENT_SECTION_GRID_VIEW_TEST_TAG = "document_section:grid_view"