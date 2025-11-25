package mega.privacy.mobile.home.presentation.home.widget.recents

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentActionTitleText
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentsTimestampText
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentsUiItem
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentsWidgetUiState
import mega.privacy.mobile.home.presentation.home.widget.recents.view.FIRST_LINE_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentsWidgetTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that title is displayed`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsView(
                    uiState = RecentsWidgetUiState(
                        recentActionItems = emptyList(),
                        isLoading = false,
                    ),
                    onNavigate = {}
                )
            }
        }

        composeRule.onNodeWithTag(TITLE_TEST_TAG).assertExists()
    }

    @Test
    fun `test that items are displayed when list is not empty`() {
        val item1 = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Document.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = System.currentTimeMillis() / 1000,
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )
        val item2 = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Image.jpg"),
            parentFolderName = LocalizedText.Literal("Photos"),
            timestamp = System.currentTimeMillis() / 1000 - 3600,
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsView(
                    uiState = RecentsWidgetUiState(
                        recentActionItems = listOf(item1, item2),
                        isLoading = false,
                    ),
                    onNavigate = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(FIRST_LINE_TEST_TAG, true)
            .assertCountEquals(2)
    }

    @Test
    fun `test that date header is displayed when dates change between items`() {
        // Use timestamps that are guaranteed to be on different days
        // 7 days ago and 14 days ago to ensure different date formatting
        val sevenDaysAgo = System.currentTimeMillis() / 1000 - (7 * 86400)
        val fourteenDaysAgo = System.currentTimeMillis() / 1000 - (14 * 86400)

        val item1 = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Document.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = sevenDaysAgo,
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )
        val item2 = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Image.jpg"),
            parentFolderName = LocalizedText.Literal("Photos"),
            timestamp = fourteenDaysAgo,
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsView(
                    uiState = RecentsWidgetUiState(
                        recentActionItems = listOf(item1, item2),
                        isLoading = false,
                    ),
                    onNavigate = {}
                )
            }
        }

        composeRule.waitForIdle()
        // Verify items are displayed
        composeRule.onAllNodesWithTag(FIRST_LINE_TEST_TAG, true)
            .assertCountEquals(2)
        // Verify date headers are shown - first item always has a header, second item has one since dates differ
        composeRule.onAllNodesWithTag(DATE_HEADER_TEST_TAG, true)
            .assertCountEquals(2)
    }

    @Test
    fun `test that date header is displayed for first item`() {
        val timestamp = System.currentTimeMillis() / 1000
        val item = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Document.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = timestamp,
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsView(
                    uiState = RecentsWidgetUiState(
                        recentActionItems = listOf(item),
                        isLoading = false,
                    ),
                    onNavigate = {}
                )
            }
        }

        composeRule.waitForIdle()
        // First item should always show a date header
        composeRule.onNodeWithTag(FIRST_LINE_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(DATE_HEADER_TEST_TAG, true).assertExists()
    }

    @Test
    fun `test that multiple items with same date show only one date header`() {
        val timestamp = System.currentTimeMillis() / 1000

        val item1 = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Document1.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = timestamp,
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )
        val item2 = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Document2.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = timestamp, // Same timestamp
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )
        val item3 = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Document3.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = timestamp, // Same timestamp
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsView(
                    uiState = RecentsWidgetUiState(
                        recentActionItems = listOf(item1, item2, item3),
                        isLoading = false,
                    ),
                    onNavigate = {}
                )
            }
        }

        composeRule.waitForIdle()
        // All items should be displayed (at least 3 items)
        composeRule.onAllNodesWithTag(FIRST_LINE_TEST_TAG, true)
            .assertCountEquals(3)
        // Only one date header should be shown since all items have the same date
        composeRule.onAllNodesWithTag(DATE_HEADER_TEST_TAG, true).assertCountEquals(1)
    }

    @Test
    fun `test that items with different dates show multiple date headers`() {
        // Use timestamps that are guaranteed to be on different days
        // 7, 14, and 21 days ago to ensure different date formatting
        val sevenDaysAgo = System.currentTimeMillis() / 1000 - (7 * 86400)
        val fourteenDaysAgo = System.currentTimeMillis() / 1000 - (14 * 86400)
        val twentyOneDaysAgo = System.currentTimeMillis() / 1000 - (21 * 86400)

        val item1 = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Item1.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = sevenDaysAgo,
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )
        val item2 = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Item2.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = fourteenDaysAgo,
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )
        val item3 = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Item3.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = twentyOneDaysAgo,
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsView(
                    uiState = RecentsWidgetUiState(
                        recentActionItems = listOf(item1, item2, item3),
                        isLoading = false,
                    ),
                    onNavigate = {}
                )
            }
        }

        composeRule.waitForIdle()
        // All items should be displayed
        composeRule.onAllNodesWithTag(FIRST_LINE_TEST_TAG, true)
            .assertCountEquals(3)
        // Verify date headers are shown - first item always has a header, subsequent items have one if dates differ
        // Since all three items have different dates, we should have 3 date headers
        composeRule.onAllNodesWithTag(DATE_HEADER_TEST_TAG, true)
            .assertCountEquals(3)
    }

    private fun createMockRecentsUiItem(
        title: RecentActionTitleText,
        parentFolderName: LocalizedText,
        timestamp: Long,
        icon: Int = IconPackR.drawable.ic_generic_medium_solid,
        shareIcon: Int? = null,
        isMediaBucket: Boolean = false,
        isUpdate: Boolean = false,
        updatedByText: LocalizedText? = null,
        userName: String? = null,
        isFavourite: Boolean = false,
        nodeLabel: NodeLabel? = null,
    ): RecentsUiItem {
        val mockBucket = RecentActionBucket(
            timestamp = timestamp,
            userEmail = "test@example.com",
            parentNodeId = NodeId(1L),
            isUpdate = isUpdate,
            isMedia = isMediaBucket,
            nodes = emptyList(),
        )
        return RecentsUiItem(
            title = title,
            icon = icon,
            shareIcon = shareIcon,
            parentFolderName = parentFolderName,
            timestampText = RecentsTimestampText(timestamp),
            isMediaBucket = isMediaBucket,
            isUpdate = isUpdate,
            updatedByText = updatedByText,
            userName = userName,
            isFavourite = isFavourite,
            nodeLabel = nodeLabel,
            bucket = mockBucket,
        )
    }
}
