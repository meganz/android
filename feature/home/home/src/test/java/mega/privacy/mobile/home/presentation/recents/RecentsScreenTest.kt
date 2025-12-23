package mega.privacy.mobile.home.presentation.recents

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.mobile.home.presentation.recents.model.RecentActionTitleText
import mega.privacy.mobile.home.presentation.recents.model.RecentsUiItem
import mega.privacy.mobile.home.presentation.recents.model.RecentsUiState
import mega.privacy.mobile.home.presentation.recents.view.FIRST_LINE_TEST_TAG
import mega.privacy.mobile.home.presentation.recents.view.RECENTS_EMPTY_TEXT_TEST_TAG
import mega.privacy.mobile.home.presentation.recents.view.RECENTS_HIDDEN_BUTTON_TEST_TAG
import mega.privacy.mobile.home.presentation.recents.view.RECENTS_HIDDEN_TEXT_TEST_TAG
import mega.privacy.mobile.home.presentation.recents.view.RECENTS_LOADING_TEST_TAG
import mega.privacy.mobile.home.presentation.recents.view.RECENTS_UPLOAD_BUTTON_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RecentsScreenTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that items are displayed when list is not empty`() {
        val item1 = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Document.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = System.currentTimeMillis() / 1000,
            icon = IconPackR.drawable.ic_generic_medium_solid,
            parentNodeId = NodeId(1L),
        )
        val item2 = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Image.jpg"),
            parentFolderName = LocalizedText.Literal("Photos"),
            timestamp = System.currentTimeMillis() / 1000 - 3600,
            icon = IconPackR.drawable.ic_generic_medium_solid,
            parentNodeId = NodeId(2L),
        )

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsScreenContent(
                    uiState = RecentsUiState(
                        recentActionItems = listOf(item1, item2),
                        isNodesLoading = false,
                        isHiddenNodeSettingsLoading = false,
                    ),
                    onFileClicked = { _, _ -> },
                    onMenuClicked = { _, _ -> },
                    onBucketClicked = { },
                    onShowRecentActivity = {},
                    onUploadClicked = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(FIRST_LINE_TEST_TAG, true)
            .assertCountEquals(2)
    }

    @Test
    fun `test that hidden view is displayed when isHideRecentsEnabled is true`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsScreenContent(
                    uiState = RecentsUiState(
                        recentActionItems = emptyList(),
                        isNodesLoading = false,
                        isHiddenNodeSettingsLoading = false,
                        isHideRecentsEnabled = true,
                    ),
                    onFileClicked = { _, _ -> },
                    onMenuClicked = { _, _ -> },
                    onBucketClicked = { },
                    onShowRecentActivity = {},
                    onUploadClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_HIDDEN_TEXT_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RECENTS_HIDDEN_BUTTON_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onShowRecentActivity is called when show activity button is clicked`() {
        var showActivityClicked = false

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsScreenContent(
                    uiState = RecentsUiState(
                        recentActionItems = emptyList(),
                        isNodesLoading = false,
                        isHiddenNodeSettingsLoading = false,
                        isHideRecentsEnabled = true,
                    ),
                    onFileClicked = { _, _ -> },
                    onMenuClicked = { _, _ -> },
                    onBucketClicked = { },
                    onShowRecentActivity = { showActivityClicked = true },
                    onUploadClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_HIDDEN_BUTTON_TEST_TAG, useUnmergedTree = true)
            .performClick()

        assertThat(showActivityClicked).isTrue()
    }

    @Test
    fun `test that empty view is displayed when isEmpty is true`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsScreenContent(
                    uiState = RecentsUiState(
                        recentActionItems = emptyList(),
                        isNodesLoading = false,
                        isHiddenNodeSettingsLoading = false,
                    ),
                    onFileClicked = { _, _ -> },
                    onMenuClicked = { _, _ -> },
                    onBucketClicked = { },
                    onShowRecentActivity = {},
                    onUploadClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_EMPTY_TEXT_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RECENTS_UPLOAD_BUTTON_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that empty view is not displayed when loading`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsScreenContent(
                    uiState = RecentsUiState(
                        recentActionItems = emptyList(),
                        isNodesLoading = true,
                        isHiddenNodeSettingsLoading = false,
                    ),
                    onFileClicked = { _, _ -> },
                    onMenuClicked = { _, _ -> },
                    onBucketClicked = { },
                    onShowRecentActivity = {},
                    onUploadClicked = {}
                )
            }
        }

        composeRule.onAllNodesWithTag(RECENTS_EMPTY_TEXT_TEST_TAG)
            .assertCountEquals(0)
    }

    @Test
    fun `test that empty view is not displayed when items exist`() {
        val item = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Document.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = System.currentTimeMillis() / 1000,
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsScreenContent(
                    uiState = RecentsUiState(
                        recentActionItems = listOf(item),
                        isNodesLoading = false,
                        isHiddenNodeSettingsLoading = false,
                    ),
                    onFileClicked = { _, _ -> },
                    onMenuClicked = { _, _ -> },
                    onBucketClicked = { },
                    onShowRecentActivity = {},
                    onUploadClicked = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(RECENTS_EMPTY_TEXT_TEST_TAG)
            .assertCountEquals(0)
    }

    @Test
    fun `test that loading view is shown when isLoading is true`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsScreenContent(
                    uiState = RecentsUiState(
                        recentActionItems = emptyList(),
                        isNodesLoading = true,
                        isHiddenNodeSettingsLoading = false,
                    ),
                    onFileClicked = { _, _ -> },
                    onMenuClicked = { _, _ -> },
                    onBucketClicked = { },
                    onShowRecentActivity = {},
                    onUploadClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_LOADING_TEST_TAG)
            .assertExists()
    }

    @Test
    fun `test that loading view is shown when isHiddenNodeSettingsLoading is true`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsScreenContent(
                    uiState = RecentsUiState(
                        recentActionItems = emptyList(),
                        isNodesLoading = false,
                        isHiddenNodeSettingsLoading = true,
                    ),
                    onFileClicked = { _, _ -> },
                    onMenuClicked = { _, _ -> },
                    onBucketClicked = { },
                    onShowRecentActivity = {},
                    onUploadClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_LOADING_TEST_TAG)
            .assertExists()
    }

    @Test
    fun `test that onUploadClicked is called when upload button is clicked in empty view`() {
        var uploadClicked = false

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsScreenContent(
                    uiState = RecentsUiState(
                        recentActionItems = emptyList(),
                        isNodesLoading = false,
                        isHiddenNodeSettingsLoading = false,
                    ),
                    onFileClicked = { _, _ -> },
                    onMenuClicked = { _, _ -> },
                    onBucketClicked = { },
                    onShowRecentActivity = {},
                    onUploadClicked = { uploadClicked = true }
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_UPLOAD_BUTTON_TEST_TAG, useUnmergedTree = true)
            .performClick()

        assertThat(uploadClicked).isTrue()
    }

    @Test
    fun `test that items are not displayed when isHideRecentsEnabled is true`() {
        val item = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Document.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = System.currentTimeMillis() / 1000,
            icon = IconPackR.drawable.ic_generic_medium_solid,
        )

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsScreenContent(
                    uiState = RecentsUiState(
                        recentActionItems = listOf(item),
                        isNodesLoading = false,
                        isHiddenNodeSettingsLoading = false,
                        isHideRecentsEnabled = true,
                    ),
                    onFileClicked = { _, _ -> },
                    onMenuClicked = { _, _ -> },
                    onBucketClicked = { },
                    onShowRecentActivity = {},
                    onUploadClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_HIDDEN_TEXT_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onAllNodesWithTag(FIRST_LINE_TEST_TAG, true)
            .assertCountEquals(0)
    }

    @Test
    fun `test that onFileClicked is called with correct node and source type`() {
        val mockNode = createMockTypedFileNode(name = "Document.pdf")
        var clickedNode: TypedFileNode? = null
        var clickedSourceType: NodeSourceType? = null

        val item = createMockRecentsUiItem(
            title = RecentActionTitleText.SingleNode("Document.pdf"),
            parentFolderName = LocalizedText.Literal("Cloud Drive"),
            timestamp = System.currentTimeMillis() / 1000,
            icon = IconPackR.drawable.ic_generic_medium_solid,
            nodes = listOf(mockNode),
            parentFolderSharesType = RecentActionsSharesType.INCOMING_SHARES,
        )

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsScreenContent(
                    uiState = RecentsUiState(
                        recentActionItems = listOf(item),
                        isNodesLoading = false,
                        isHiddenNodeSettingsLoading = false,
                    ),
                    onFileClicked = { node, sourceType ->
                        clickedNode = node
                        clickedSourceType = sourceType
                    },
                    onMenuClicked = { _, _ -> },
                    onBucketClicked = { },
                    onShowRecentActivity = {},
                    onUploadClicked = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(FIRST_LINE_TEST_TAG, true)[0]
            .performClick()

        assertThat(clickedNode).isEqualTo(mockNode)
        assertThat(clickedSourceType).isEqualTo(NodeSourceType.INCOMING_SHARES)
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
        nodeLabel: NodeLabel? = null,
        nodes: List<TypedFileNode> = emptyList(),
        parentFolderSharesType: RecentActionsSharesType = RecentActionsSharesType.NONE,
        isSensitive: Boolean = false,
        parentNodeId: NodeId = NodeId(1L),
    ): RecentsUiItem {
        val dateTimestamp = Instant.ofEpochSecond(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toEpochSecond()
        val mockBucket = RecentActionBucket(
            identifier = "$timestamp $isUpdate",
            timestamp = timestamp,
            dateTimestamp = dateTimestamp,
            userEmail = "test@example.com",
            parentNodeId = parentNodeId,
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
