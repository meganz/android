package mega.privacy.mobile.home.presentation.recents.view

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.mobile.home.presentation.recents.model.RecentActionTitleText
import mega.privacy.mobile.home.presentation.recents.model.RecentsTimestampText
import mega.privacy.mobile.home.presentation.recents.model.RecentsUiItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class RecentsLazyListViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that onFileClicked is not called when firstNode is null for single node item`() {
        var clickedNode: TypedFileNode? = null
        var clickedSourceType: NodeSourceType? = null

        val item = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Document.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = System.currentTimeMillis() / 1000,
            icon = IconPackR.drawable.ic_generic_medium_solid,
            nodes = emptyList(), // Empty nodes list means firstNode will be null
            parentFolderSharesType = RecentActionsSharesType.NONE,
        )

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsLazyListView(
                    items = listOf(item),
                    onFileClicked = { node, sourceType ->
                        clickedNode = node
                        clickedSourceType = sourceType
                    }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(FIRST_LINE_TEST_TAG, true)[0]
            .performClick()

        assertThat(clickedNode).isNull()
        assertThat(clickedSourceType).isNull()
    }

    @Test
    fun `test that onFileClicked is called when firstNode is not null for single node item`() {
        val mockNode = createMockTypedFileNode(name = "Document.pdf")
        var clickedNode: TypedFileNode? = null
        var clickedSourceType: NodeSourceType? = null

        val item = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Document.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = System.currentTimeMillis() / 1000,
            icon = IconPackR.drawable.ic_generic_medium_solid,
            nodes = listOf(mockNode),
            parentFolderSharesType = RecentActionsSharesType.NONE,
        )

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsLazyListView(
                    items = listOf(item),
                    onFileClicked = { node, sourceType ->
                        clickedNode = node
                        clickedSourceType = sourceType
                    }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(FIRST_LINE_TEST_TAG, true)[0]
            .performClick()

        assertThat(clickedNode).isEqualTo(mockNode)
        assertThat(clickedSourceType).isEqualTo(NodeSourceType.CLOUD_DRIVE)
    }

    @Test
    fun `test that items are displayed in list`() {
        val item1 = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Document1.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = System.currentTimeMillis() / 1000,
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )
        val item2 = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Document2.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = System.currentTimeMillis() / 1000 - 3600,
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsLazyListView(
                    items = listOf(item1, item2),
                    onFileClicked = { _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(FIRST_LINE_TEST_TAG, true)
            .assertCountEquals(2)
    }

    private fun createMockTypedFileNode(
        name: String = "testFile.txt",
    ): TypedFileNode = mock {
        on { it.name }.thenReturn(name)
        on { it.id }.thenReturn(NodeId(1L))
        on { it.type }.thenReturn(TextFileTypeInfo("text/plain", "txt"))
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
        nodeLabel: mega.privacy.android.domain.entity.NodeLabel? = null,
        nodes: List<TypedFileNode> = emptyList(),
        parentFolderSharesType: RecentActionsSharesType = RecentActionsSharesType.NONE,
        isSensitive: Boolean = false,
    ): RecentsUiItem {
        val mockBucket = RecentActionBucket(
            identifier = "$timestamp $userName $isMediaBucket",
            timestamp = timestamp,
            userEmail = "test@example.com",
            parentNodeId = NodeId(1L),
            isUpdate = isUpdate,
            isMedia = isMediaBucket,
            nodes = nodes,
            parentFolderSharesType = parentFolderSharesType,
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
            isSingleNode = !isMediaBucket,
            isSensitive = isSensitive
        )
    }
}
