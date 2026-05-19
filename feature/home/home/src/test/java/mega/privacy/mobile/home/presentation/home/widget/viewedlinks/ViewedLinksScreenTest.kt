package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.RecentlyViewedLinkType
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.home.widget.viewedlinks.view.VIEWED_LINK_LOADING_ITEM_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewedLinksScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val navigationIconContentDescription = "Navigation Icon"

    private val fileLinkItem = ViewedLinkUiItem(
        viewedLink = ViewedLink(
            nodeHandle = 1L,
            name = "Document.pdf",
            linkUrl = "https://mega.nz/file/abc",
            type = RecentlyViewedLinkType.FileLink,
        ),
        iconRes = iconPackR.drawable.ic_pdf_medium_solid,
        previewPath = null,
    )

    private val folderLinkItem = ViewedLinkUiItem(
        viewedLink = ViewedLink(
            nodeHandle = 2L,
            name = "Recipes",
            linkUrl = "https://mega.nz/folder/def",
            type = RecentlyViewedLinkType.FolderLink,
        ),
        iconRes = iconPackR.drawable.ic_folder_users_small_solid,
        previewPath = null,
    )

    @Test
    fun `test that title is displayed`() {
        setContent(items = null)

        val title = context.getString(sharedR.string.home_widget_viewed_links_section_header)
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `test that loading items are displayed while refresh is loading`() {
        setContent(items = null)

        composeRule.onAllNodesWithTag(VIEWED_LINK_LOADING_ITEM_TEST_TAG)
            .assertCountEquals(6)
    }

    @Test
    fun `test that no items are displayed when the paged list is empty`() {
        setContent(items = emptyList())

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(VIEWED_LINKS_ITEM_TEST_TAG)
            .assertCountEquals(0)
        composeRule.onAllNodesWithTag(VIEWED_LINK_LOADING_ITEM_TEST_TAG)
            .assertCountEquals(0)
    }

    @Test
    fun `test that items are displayed when the paged list has items`() {
        setContent(items = listOf(fileLinkItem, folderLinkItem))

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(VIEWED_LINKS_ITEM_TEST_TAG)
            .assertCountEquals(2)
    }

    @Test
    fun `test that loading items are not displayed when items are loaded`() {
        setContent(items = listOf(fileLinkItem))

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(VIEWED_LINK_LOADING_ITEM_TEST_TAG)
            .assertCountEquals(0)
    }

    @Test
    fun `test that onFileLinkClicked is called with correct url when a file link item is clicked`() {
        var clickedUrl: String? = null

        setContent(
            items = listOf(fileLinkItem),
            onFileLinkClicked = { clickedUrl = it },
        )

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(VIEWED_LINKS_ITEM_TEST_TAG)[0].performClick()

        assertThat(clickedUrl).isEqualTo(fileLinkItem.viewedLink.linkUrl)
    }

    @Test
    fun `test that onFolderLinkClicked is called with correct url when a folder link item is clicked`() {
        var clickedUrl: String? = null

        setContent(
            items = listOf(folderLinkItem),
            onFolderLinkClicked = { clickedUrl = it },
        )

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(VIEWED_LINKS_ITEM_TEST_TAG)[0].performClick()

        assertThat(clickedUrl).isEqualTo(folderLinkItem.viewedLink.linkUrl)
    }

    @Test
    fun `test that onFolderLinkClicked is not called when a file link item is clicked`() {
        var folderClicked = false

        setContent(
            items = listOf(fileLinkItem),
            onFolderLinkClicked = { folderClicked = true },
        )

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(VIEWED_LINKS_ITEM_TEST_TAG)[0].performClick()

        assertThat(folderClicked).isFalse()
    }

    @Test
    fun `test that onFileLinkClicked is not called when a folder link item is clicked`() {
        var fileClicked = false

        setContent(
            items = listOf(folderLinkItem),
            onFileLinkClicked = { fileClicked = true },
        )

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(VIEWED_LINKS_ITEM_TEST_TAG)[0].performClick()

        assertThat(fileClicked).isFalse()
    }

    @Test
    fun `test that onBack is invoked when navigation icon is clicked`() {
        var backClicked = false
        setContent(
            items = listOf(fileLinkItem),
            onBack = { backClicked = true },
        )

        composeRule.onNodeWithContentDescription(navigationIconContentDescription)
            .performClick()

        assertThat(backClicked).isTrue()
    }

    @Test
    fun `test that options sheet is not displayed by default`() {
        setContent(items = listOf(fileLinkItem))

        composeRule.onAllNodesWithTag(CLEAR_HISTORY_TAG, useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun `test that clear history option is displayed when more menu is clicked`() {
        setContent(items = listOf(fileLinkItem))

        composeRule.onNodeWithTag(CommonMenuAction.More.testTag).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(CLEAR_HISTORY_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that clear confirmation dialog is not displayed by default`() {
        setContent(items = listOf(fileLinkItem))

        composeRule.onAllNodesWithTag(CLEAR_HISTORY_DIALOG_TAG, useUnmergedTree = true)
            .assertCountEquals(0)
    }

    /**
     * @param items List of items to render; `null` keeps the refresh state in `Loading`
     *   (no PagingData ever emitted), which is how the loading skeleton is exercised.
     */
    private fun setContent(
        items: List<ViewedLinkUiItem>?,
        uiState: ViewedLinksUiState = ViewedLinksUiState(),
        onFolderLinkClicked: (String) -> Unit = {},
        onFileLinkClicked: (String) -> Unit = {},
        onClearAllLinks: () -> Unit = {},
        onSortOptionSelected: (NodeSortConfiguration) -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                val lazyItems = lazyItemsOf(items)
                ViewedLinksScreen(
                    uiState = uiState,
                    lazyItems = lazyItems,
                    onFolderLinkClicked = onFolderLinkClicked,
                    onFileLinkClicked = onFileLinkClicked,
                    onClearAllLinks = onClearAllLinks,
                    onSortOptionSelected = onSortOptionSelected,
                    onBack = onBack,
                )
            }
        }
    }

    @Composable
    private fun lazyItemsOf(items: List<ViewedLinkUiItem>?): LazyPagingItems<ViewedLinkUiItem> {
        val flow = remember(items) {
            when (items) {
                null -> emptyFlow()
                else -> MutableStateFlow(PagingData.from(items))
            }
        }
        return flow.collectAsLazyPagingItems()
    }
}
