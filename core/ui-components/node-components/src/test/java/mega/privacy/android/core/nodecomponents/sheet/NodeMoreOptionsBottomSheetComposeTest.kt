package mega.privacy.android.core.nodecomponents.sheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.core.nodecomponents.sheet.nodeactions.ERROR_NODE_ICON_TAG
import mega.privacy.android.core.nodecomponents.sheet.nodeactions.HELP_ICON_TAG
import mega.privacy.android.core.nodecomponents.sheet.nodeactions.NODE_ICON_TAG
import mega.privacy.android.core.nodecomponents.sheet.nodeactions.NodeMoreOptionsBottomSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeMoreOptionsBottomSheetComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()


    @OptIn(ExperimentalMaterial3Api::class)
    private fun setContent(
        options: List<NodeSelectionAction> = emptyList(),
        onActionPressed: (NodeSelectionAction) -> Unit = {},
        onHelpClicked: (NodeSelectionAction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { true }
            )

            LaunchedEffect(Unit) {
                delay(500)
                sheetState.show()
            }

            NodeMoreOptionsBottomSheet(
                actions = options,
                sheetState = sheetState,
                onActionPressed = onActionPressed,
                onHelpClicked = onHelpClicked
            )
        }
    }

    @Test
    fun `test that options are displayed correctly`() {
        val options = NodeSelectionAction.defaults

        setContent(options = options)

        options.forEach { option ->
            composeTestRule.onNodeWithTag(option.testTag)
                .assertExists("Option ${option.testTag} should be displayed")
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that help icon is displayed for options with help`() {
        val option = NodeSelectionAction.defaults.first { it is NodeSelectionAction.Hide }

        setContent(options = listOf(option))

        composeTestRule.onNodeWithTag(HELP_ICON_TAG)
            .assertExists("Help icon for ${option.testTag} should be displayed")
            .assertIsDisplayed()
    }

    @Test
    fun `test that error icon is displayed for options with error text color`() {
        val option = NodeSelectionAction.defaults.first { it is NodeSelectionAction.RubbishBin }

        setContent(options = listOf(option))

        composeTestRule.onNodeWithTag(testTag = ERROR_NODE_ICON_TAG, useUnmergedTree = true)
            .assertExists("Error icon for ${option.testTag} should be displayed")
            .assertIsDisplayed()
    }

    @Test
    fun `test that regular icon is displayed for options without error text color`() {
        val option = NodeSelectionAction.defaults.first { it != NodeSelectionAction.RubbishBin }

        setContent(options = listOf(option))

        composeTestRule.onNodeWithTag(testTag = NODE_ICON_TAG, useUnmergedTree = true)
            .assertExists("Regular icon for ${option.testTag} should be displayed")
            .assertIsDisplayed()
    }
}