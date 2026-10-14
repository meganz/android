package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.node.RecentlyViewedLinkType
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.mobile.analytics.event.ViewedLinksViewAllButtonPressedEvent
import mega.privacy.mobile.analytics.event.ViewedLinksWidgetFileLinkItemSelectedEvent
import mega.privacy.mobile.analytics.event.ViewedLinksWidgetFolderLinkItemSelectedEvent
import mega.privacy.mobile.home.presentation.home.widget.viewedlinks.view.VIEWED_LINK_LOADING_ITEM_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewedLinksWidgetTest {

    private val composeRule = createComposeRule()

    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeRule)

    private val fileLinkItem = viewedLinkItem(
        nodeHandle = 1L,
        type = RecentlyViewedLinkType.FileLink,
    )

    private val folderLinkItem = viewedLinkItem(
        nodeHandle = 2L,
        type = RecentlyViewedLinkType.FolderLink,
    )

    @Test
    fun `test that file link item tracks the widget file link event when clicked`() {
        var clickedUrl: String? = null
        setContent(
            items = listOf(fileLinkItem),
            onFileLinkClicked = { clickedUrl = it },
        )

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(VIEWED_LINKS_ITEM_TEST_TAG)[0].performClick()

        assertThat(clickedUrl).isEqualTo(fileLinkItem.viewedLink.linkUrl)
        assertThat(analyticsRule.events)
            .containsExactly(ViewedLinksWidgetFileLinkItemSelectedEvent)
    }

    @Test
    fun `test that folder link item tracks the widget folder link event when clicked`() {
        var clickedUrl: String? = null
        setContent(
            items = listOf(folderLinkItem),
            onFolderLinkClicked = { clickedUrl = it },
        )

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(VIEWED_LINKS_ITEM_TEST_TAG)[0].performClick()

        assertThat(clickedUrl).isEqualTo(folderLinkItem.viewedLink.linkUrl)
        assertThat(analyticsRule.events)
            .containsExactly(ViewedLinksWidgetFolderLinkItemSelectedEvent)
    }

    @Test
    fun `test that header tracks the view all event when clicked with more than the preview count`() {
        var viewAllClicked = false
        setContent(
            items = (1L..5L).map { viewedLinkItem(nodeHandle = it) },
            onViewAllClicked = { viewAllClicked = true },
        )

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(VIEWED_LINKS_TITLE_TEST_TAG, useUnmergedTree = true)
            .performClick()

        assertThat(viewAllClicked).isTrue()
        assertThat(analyticsRule.events).containsExactly(ViewedLinksViewAllButtonPressedEvent)
    }

    @Test
    fun `test that header does not track the view all event when clicked with fewer items than the preview count`() {
        var viewAllClicked = false
        setContent(
            items = listOf(fileLinkItem),
            onViewAllClicked = { viewAllClicked = true },
        )

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(VIEWED_LINKS_TITLE_TEST_TAG, useUnmergedTree = true)
            .performClick()

        assertThat(viewAllClicked).isFalse()
        assertThat(analyticsRule.events).isEmpty()
    }

    @Test
    fun `test that loading item tracks the view all event when clicked`() {
        var viewAllClicked = false
        setContent(
            items = null,
            onViewAllClicked = { viewAllClicked = true },
        )

        composeRule.onNodeWithTag(VIEWED_LINK_LOADING_ITEM_TEST_TAG, useUnmergedTree = true)
            .performClick()

        assertThat(viewAllClicked).isTrue()
        assertThat(analyticsRule.events).containsExactly(ViewedLinksViewAllButtonPressedEvent)
    }

    private fun viewedLinkItem(
        nodeHandle: Long,
        type: RecentlyViewedLinkType = RecentlyViewedLinkType.FileLink,
    ) = ViewedLinkUiItem(
        viewedLink = ViewedLink(
            nodeHandle = nodeHandle,
            name = "Link $nodeHandle",
            linkUrl = "https://mega.nz/link/$nodeHandle",
            type = type,
        ),
        iconRes = iconPackR.drawable.ic_pdf_medium_solid,
        previewPath = null,
    )

    /**
     * @param items List of items to render; `null` keeps the refresh state in `Loading`
     *   (no PagingData ever emitted), which is how the loading skeleton is exercised.
     */
    private fun setContent(
        items: List<ViewedLinkUiItem>?,
        onFolderLinkClicked: (String) -> Unit = {},
        onFileLinkClicked: (String) -> Unit = {},
        onViewAllClicked: () -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                ViewedLinksView(
                    lazyItems = lazyItemsOf(items),
                    onFolderLinkClicked = onFolderLinkClicked,
                    onFileLinkClicked = onFileLinkClicked,
                    onViewAllClicked = onViewAllClicked,
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
