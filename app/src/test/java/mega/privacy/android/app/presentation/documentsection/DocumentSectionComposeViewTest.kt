package mega.privacy.android.app.presentation.documentsection

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.documentsection.model.DocumentSectionUiState
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntity
import mega.privacy.android.app.presentation.documentsection.view.DOCUMENT_SECTION_EMPTY_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.documentsection.view.DOCUMENT_SECTION_FAB_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.documentsection.view.DOCUMENT_SECTION_GRID_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.documentsection.view.DOCUMENT_SECTION_LIST_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.documentsection.view.DOCUMENT_SECTION_PROGRESS_BAR_TEST_TAG
import mega.privacy.android.app.presentation.documentsection.view.DocumentSectionComposeView
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.icon.pack.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class DocumentSectionComposeViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        uiState: DocumentSectionUiState = DocumentSectionUiState(),
        onChangeViewTypeClick: () -> Unit = {},
        onClick: (item: DocumentUiEntity, index: Int) -> Unit = { _, _ -> },
        onSortOrderClick: () -> Unit = {},
        onMenuClick: (DocumentUiEntity) -> Unit = {},
        onLongClick: (item: DocumentUiEntity, index: Int) -> Unit = { _, _ -> },
        onAddDocumentClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            DocumentSectionComposeView(
                modifier = Modifier,
                uiState = uiState,
                onChangeViewTypeClick = onChangeViewTypeClick,
                onClick = onClick,
                onSortOrderClick = onSortOrderClick,
                onMenuClick = onMenuClick,
                onLongClick = onLongClick,
                onAddDocumentClick = onAddDocumentClick,
            )
        }
    }

    @Test
    fun `test that progress bar is displayed when loading`() {
        setComposeContent(uiState = DocumentSectionUiState(isLoading = true))

        composeTestRule.onNodeWithTag(DOCUMENT_SECTION_PROGRESS_BAR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that empty view is displayed when items are empty`() {
        setComposeContent(uiState = DocumentSectionUiState(isLoading = false))

        composeTestRule.onNodeWithTag(DOCUMENT_SECTION_EMPTY_VIEW_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that list view is displayed when ViewType is list`() {
        setComposeContent(
            uiState = DocumentSectionUiState(
                allDocuments = getItems(),
                isLoading = false,
                currentViewType = ViewType.LIST
            )
        )

        composeTestRule.onNodeWithTag(DOCUMENT_SECTION_LIST_VIEW_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that grid view is displayed when ViewType is grid`() {
        setComposeContent(
            uiState = DocumentSectionUiState(
                allDocuments = getItems(),
                isLoading = false,
                currentViewType = ViewType.GRID
            )
        )

        composeTestRule.onNodeWithTag(DOCUMENT_SECTION_GRID_VIEW_TEST_TAG).assertIsDisplayed()
    }

    private fun getItems() = listOf(
        getDocumentItem(NodeId(1)),
        getDocumentItem(NodeId(2)),
        getDocumentItem(NodeId(3))
    )

    private fun getDocumentItem(
        documentId: NodeId,
    ) = mock<DocumentUiEntity> {
        on { id }.thenReturn(documentId)
        on { name }.thenReturn("document.txt")
        on { size }.thenReturn(1000)
        on { modificationTime }.thenReturn(100000)
        on { label }.thenReturn(0)
        on { icon }.thenReturn(R.drawable.ic_text_medium_solid)
        on { fileTypeInfo }.thenReturn(
            TextFileTypeInfo(
                mimeType = "text/plain",
                extension = "txt"
            )
        )
    }

    @Test
    fun `test that onAddDocumentClick is invoked when adding fab button is clicked`() {
        val onAddDocumentClick: () -> Unit = mock()
        setComposeContent(onAddDocumentClick = onAddDocumentClick)

        composeTestRule.onNodeWithTag(DOCUMENT_SECTION_FAB_BUTTON_TEST_TAG).apply {
            assertIsDisplayed()
            performClick()
        }
        verify(onAddDocumentClick).invoke()
    }
}