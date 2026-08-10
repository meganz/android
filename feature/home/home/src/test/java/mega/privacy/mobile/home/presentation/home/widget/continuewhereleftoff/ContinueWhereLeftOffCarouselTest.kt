package mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContinueWhereLeftOffCarouselTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val pdfItem = ContinueWhereLeftOffItem(
        nodeHandle = 1L,
        title = "Document.pdf",
        type = RecentlyUsedType.PDF,
        lastAccessedTimestamp = 1712880000L,
    )

    @Test
    fun `test that title is displayed when items is empty`() {
        setContent(items = emptyList())

        val title = context.getString(sharedR.string.home_widget_continue_where_left_off)
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `test that title is displayed when items is not empty`() {
        setContent(items = listOf(pdfItem))

        val title = context.getString(sharedR.string.home_widget_continue_where_left_off)
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `test that empty view is displayed when items is empty`() {
        setContent(items = emptyList())

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_EMPTY_TEXT_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that empty view is not displayed when items is not empty`() {
        setContent(items = listOf(pdfItem))

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_EMPTY_TEXT_TEST_TAG)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that loading view is displayed when isLoading is true`() {
        setContent(items = emptyList(), isLoading = true)

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_LOADING_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that empty view is not displayed when isLoading is true`() {
        setContent(items = emptyList(), isLoading = true)

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_EMPTY_TEXT_TEST_TAG)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that loading view is not displayed when isLoading is false`() {
        setContent(items = emptyList(), isLoading = false)

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_LOADING_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test that chevron is hidden when items is empty`() {
        setContent(items = emptyList())

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_CHEVRON_TEST_TAG)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that chevron is displayed when items is not empty`() {
        setContent(items = listOf(pdfItem))

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_CHEVRON_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onViewAllClick is invoked when chevron is clicked`() {
        var viewAllClicked = false
        setContent(
            items = listOf(pdfItem),
            onViewAllClick = { viewAllClicked = true },
        )

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_CHEVRON_TEST_TAG).performClick()

        assertThat(viewAllClicked).isTrue()
    }

    @Test
    fun `test that carousel scrolls to start when a new most recent item is added`() {
        val initialItems = buildItems(count = 8)
        val items = setContentWithMutableItems(initialItems)
        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_LIST_TEST_TAG)
            .performScrollToNode(hasText("Item 8"))
        composeRule.onNodeWithText("Item 1").assertDoesNotExist()

        val newItem = ContinueWhereLeftOffItem(
            nodeHandle = 99L,
            title = "New item",
            type = RecentlyUsedType.Video,
            lastAccessedTimestamp = 1712990000L,
        )
        items.value = listOf(newItem) + initialItems
        composeRule.waitForIdle()

        composeRule.onNodeWithText("New item").assertIsDisplayed()
    }

    @Test
    fun `test that carousel scrolls to start when an existing item becomes most recent`() {
        val initialItems = buildItems(count = 8)
        val items = setContentWithMutableItems(initialItems)
        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_LIST_TEST_TAG)
            .performScrollToNode(hasText("Item 8"))

        // Item 5 is accessed again and moves to the front of the list
        items.value = listOf(initialItems[4]) + (initialItems - initialItems[4])
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Item 5").assertIsDisplayed()
        composeRule.onNodeWithText("Item 1").assertIsDisplayed()
    }

    @Test
    fun `test that more tile is not displayed when items are within the visible limit`() {
        setContent(items = buildItems(count = 8))

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_MORE_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test that more tile is displayed when items exceed the visible limit`() {
        setContent(items = buildItems(count = 9))

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_LIST_TEST_TAG)
            .performScrollToNode(hasTestTag(CONTINUE_WHERE_LEFT_OFF_MORE_TEST_TAG))
        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_MORE_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that only the visible limit of items is shown when more tile is displayed`() {
        setContent(items = buildItems(count = 9))

        // The 9th item is replaced by the "More" tile, so it is never rendered in the carousel
        composeRule.onNodeWithText("Item 9").assertDoesNotExist()
    }

    @Test
    fun `test that onViewAllClick is invoked when more tile is clicked`() {
        var viewAllClicked = false
        setContent(
            items = buildItems(count = 9),
            onViewAllClick = { viewAllClicked = true },
        )

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_LIST_TEST_TAG)
            .performScrollToNode(hasTestTag(CONTINUE_WHERE_LEFT_OFF_MORE_TEST_TAG))
        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_MORE_TEST_TAG).performClick()

        assertThat(viewAllClicked).isTrue()
    }

    private fun buildItems(count: Int) = (1L..count).map { handle ->
        ContinueWhereLeftOffItem(
            nodeHandle = handle,
            title = "Item $handle",
            type = RecentlyUsedType.PDF,
            lastAccessedTimestamp = 1712880000L + handle,
        )
    }

    private fun setContentWithMutableItems(
        initialItems: List<ContinueWhereLeftOffItem>,
    ): MutableState<List<ContinueWhereLeftOffItem>> {
        val items = mutableStateOf(initialItems)
        composeRule.setContent {
            AndroidThemeForPreviews {
                ContinueWhereLeftOffCarousel(
                    items = items.value,
                    isLoading = false,
                    onItemClick = {},
                    onViewAllClick = {},
                )
            }
        }
        return items
    }

    private fun setContent(
        items: List<ContinueWhereLeftOffItem>,
        isLoading: Boolean = false,
        onItemClick: (ContinueWhereLeftOffItem) -> Unit = {},
        onViewAllClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                ContinueWhereLeftOffCarousel(
                    items = items,
                    isLoading = isLoading,
                    onItemClick = onItemClick,
                    onViewAllClick = onViewAllClick,
                )
            }
        }
    }
}
