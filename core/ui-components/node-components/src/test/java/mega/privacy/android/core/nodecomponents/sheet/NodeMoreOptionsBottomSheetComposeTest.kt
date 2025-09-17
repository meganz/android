package mega.privacy.android.core.nodecomponents.sheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
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

    val previewActions = listOf(
        DownloadMenuAction(),
        ManageLinkMenuAction(),
        HideMenuAction(),
        MoveMenuAction(),
        CopyMenuAction(),
        TrashMenuAction()
    )

    @OptIn(ExperimentalMaterial3Api::class)
    private fun setContent(
        options: List<MenuActionWithIcon> = emptyList(),
        onActionPressed: (MenuActionWithIcon) -> Unit = {},
        onHelpClicked: (MenuActionWithIcon) -> Unit = {},
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
        setContent(options = previewActions)

        previewActions.forEach { option ->
            composeTestRule.onNodeWithTag(option.testTag)
                .assertExists("Option ${option.testTag} should be displayed")
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that help icon is displayed for options with help`() {
        val option = previewActions.first { it is HideMenuAction }

        setContent(options = listOf(option))

        composeTestRule.onNodeWithTag(HELP_ICON_TAG)
            .assertExists("Help icon for ${option.testTag} should be displayed")
            .assertIsDisplayed()
    }

    @Test
    fun `test that error icon is displayed for options with error text color`() {
        val option = previewActions.first { it is TrashMenuAction }

        setContent(options = listOf(option))

        composeTestRule.onNodeWithTag(testTag = ERROR_NODE_ICON_TAG, useUnmergedTree = true)
            .assertExists("Error icon for ${option.testTag} should be displayed")
            .assertIsDisplayed()
    }

    @Test
    fun `test that regular icon is displayed for options without error text color`() {
        val option = previewActions.first { it !is TrashMenuAction }

        setContent(options = listOf(option))

        composeTestRule.onNodeWithTag(testTag = NODE_ICON_TAG, useUnmergedTree = true)
            .assertExists("Regular icon for ${option.testTag} should be displayed")
            .assertIsDisplayed()
    }
}