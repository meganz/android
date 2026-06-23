package mega.privacy.android.feature.cloudexplorer.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class CloudExplorerViewItemsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the list item shows the node title`() {
        setContent { ExplorerNodeListItemUnderTest {} }

        composeTestRule.onNodeWithText(FOLDER_NAME).assertIsDisplayed()
    }

    @Test
    fun `test that clicking the list item invokes the callback`() {
        var clicked = false
        setContent { ExplorerNodeListItemUnderTest { clicked = true } }

        composeTestRule.onNodeWithText(FOLDER_NAME).performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that the grid item shows the node name`() {
        setContent { ExplorerNodeGridItemUnderTest {} }

        composeTestRule.onNodeWithText(FOLDER_NAME).assertIsDisplayed()
    }

    @Test
    fun `test that clicking the grid item invokes the callback`() {
        var clicked = false
        setContent { ExplorerNodeGridItemUnderTest { clicked = true } }

        composeTestRule.onNodeWithText(FOLDER_NAME).performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that clicking a restricted list item still invokes the callback`() {
        var clicked = false
        setContent {
            ExplorerNodeListItemUnderTest(
                restrictedNodeIds = setOf(NodeId(1L)),
                onItemClicked = { clicked = true },
            )
        }

        composeTestRule.onNodeWithText(FOLDER_NAME).performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that clicking an already added list item does not invoke the callback`() {
        var clicked = false
        setContent {
            ExplorerNodeListItemUnderTest(
                disabledNodeIds = setOf(NodeId(1L)),
                onItemClicked = { clicked = true },
            )
        }

        composeTestRule.onNodeWithText(FOLDER_NAME).performClick()

        assertThat(clicked).isFalse()
    }

    @Composable
    private fun ExplorerNodeListItemUnderTest(
        disabledNodeIds: Set<NodeId> = emptySet(),
        restrictedNodeIds: Set<NodeId> = emptySet(),
        onItemClicked: () -> Unit,
    ) {
        ExplorerNodeListItem(
            item = previewFolderNodeUiItem(1L, name = FOLDER_NAME),
            isSelected = false,
            isSelectionModeEnabled = false,
            isHiddenNodesEnabled = false,
            videosOnly = false,
            disabledNodeIds = disabledNodeIds,
            restrictedNodeIds = restrictedNodeIds,
            onItemClicked = onItemClicked,
        )
    }

    @Composable
    private fun ExplorerNodeGridItemUnderTest(onItemClicked: () -> Unit) {
        ExplorerNodeGridItem(
            item = previewFolderNodeUiItem(1L, name = FOLDER_NAME),
            isSelected = false,
            isSelectionModeEnabled = false,
            isHiddenNodesEnabled = false,
            videosOnly = false,
            disabledNodeIds = emptySet(),
            onItemClicked = onItemClicked,
        )
    }

    private fun setContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            AndroidThemeForPreviews {
                content()
            }
        }
    }

    private companion object {
        const val FOLDER_NAME = "Sample folder"
    }
}
