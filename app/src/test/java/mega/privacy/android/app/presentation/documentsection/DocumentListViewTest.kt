package mega.privacy.android.app.presentation.documentsection

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.test.FakeImageLoaderEngine
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntity
import mega.privacy.android.app.presentation.documentsection.view.DOCUMENT_SECTION_ITEM_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.documentsection.view.DocumentListView
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.icon.pack.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoilApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DocumentListViewTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        val engine = FakeImageLoaderEngine.Builder().build()
        val imageLoader = ImageLoader.Builder(composeTestRule.activity)
            .components { add(engine) }
            .build()
        Coil.setImageLoader(imageLoader)
    }

    private fun setComposeContent(
        items: List<DocumentUiEntity>,
        lazyListState: LazyListState = LazyListState(),
        sortOrder: String = "",
        modifier: Modifier = Modifier,
        onChangeViewTypeClick: () -> Unit = {},
        onClick: (item: DocumentUiEntity, index: Int) -> Unit,
        onMenuClick: (DocumentUiEntity) -> Unit = {},
        onSortOrderClick: () -> Unit = {},
        onLongClick: ((item: DocumentUiEntity, index: Int) -> Unit) = { _, _ -> },
    ) {
        composeTestRule.setContent {
            DocumentListView(
                items = items,
                accountType = null,
                lazyListState = lazyListState,
                sortOrder = sortOrder,
                modifier = modifier,
                onChangeViewTypeClick = onChangeViewTypeClick,
                onClick = onClick,
                onMenuClick = onMenuClick,
                onSortOrderClick = onSortOrderClick,
                onLongClick = onLongClick,
                isSelectionMode = false,
            )
        }
    }

    @Test
    fun `test that onClick is invoked when the item is clicked`() {
        val document1 = getDocumentItem(NodeId(1))
        val document2 = getDocumentItem(NodeId(2))
        val document3 = getDocumentItem(NodeId(3))
        val onClick: (DocumentUiEntity, Int) -> Unit = mock()
        setComposeContent(items = listOf(document1, document2, document3), onClick = onClick)

        composeTestRule.onNodeWithTag(DOCUMENT_SECTION_ITEM_VIEW_TEST_TAG + "0").performClick()
        verify(onClick).invoke(document1, 0)
        composeTestRule.onNodeWithTag(DOCUMENT_SECTION_ITEM_VIEW_TEST_TAG + "1").performClick()
        verify(onClick).invoke(document2, 1)
        composeTestRule.onNodeWithTag(DOCUMENT_SECTION_ITEM_VIEW_TEST_TAG + "2").performClick()
        verify(onClick).invoke(document3, 2)
    }

    private fun getDocumentItem(
        documentId: NodeId
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
}