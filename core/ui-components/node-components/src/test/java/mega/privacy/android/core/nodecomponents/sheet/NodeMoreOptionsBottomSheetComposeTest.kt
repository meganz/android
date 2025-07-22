package mega.privacy.android.core.nodecomponents.sheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.sheet.nodeactions.ERROR_NODE_ICON_TAG
import mega.privacy.android.core.nodecomponents.sheet.nodeactions.HELP_ICON_TAG
import mega.privacy.android.core.nodecomponents.sheet.nodeactions.NODE_ICON_TAG
import mega.privacy.android.core.nodecomponents.sheet.nodeactions.NodeActionUiOption
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
        options: List<NodeActionUiOption> = emptyList(),
        onOptionSelected: (NodeActionUiOption) -> Unit = {},
        onHelpClicked: (NodeActionUiOption) -> Unit = {},
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
                options = options,
                sheetState = sheetState,
                onOptionSelected = onOptionSelected,
                onHelpClicked = onHelpClicked
            )
        }
    }

    @Test
    fun `test that options are displayed correctly`() {
        val options = NodeActionUiOption.defaults

        setContent(options = options)

        options.forEach { option ->
            composeTestRule.onNodeWithTag(option.key)
                .assertExists("Option ${option.key} should be displayed")
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that help icon is displayed for options with help`() {
        val option = NodeActionUiOption.defaults.first { it.showHelpButton }

        setContent(options = listOf(option))

        composeTestRule.onNodeWithTag(HELP_ICON_TAG)
            .assertExists("Help icon for ${option.key} should be displayed")
            .assertIsDisplayed()
    }

    @Test
    fun `test that error icon is displayed for options with error text color`() {
        val option = NodeActionUiOption.defaults.first { it.textColor == TextColor.Error }

        setContent(options = listOf(option))

        composeTestRule.onNodeWithTag(testTag = ERROR_NODE_ICON_TAG, useUnmergedTree = true)
            .assertExists("Error icon for ${option.key} should be displayed")
            .assertIsDisplayed()
    }

    @Test
    fun `test that regular icon is displayed for options without error text color`() {
        val option = NodeActionUiOption.defaults.first { it.textColor != TextColor.Error }

        setContent(options = listOf(option))

        composeTestRule.onNodeWithTag(testTag = NODE_ICON_TAG, useUnmergedTree = true)
            .assertExists("Regular icon for ${option.key} should be displayed")
            .assertIsDisplayed()
    }
}