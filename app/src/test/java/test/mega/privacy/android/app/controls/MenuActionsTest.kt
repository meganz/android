package test.mega.privacy.android.app.controls

import android.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.ui.controls.menus.MenuActions
import mega.privacy.android.core.ui.controls.menus.TAG_MENU_ACTIONS_SHOW_MORE
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuActionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that when the same amount of buttons with icon as the max actions to show are added then all icon's actions are shown`() {
        val actions = List(defaultAmount) {
            MenuActionWithIconForTest("Action $it")
        }
        composeTestRule.setContent {
            MenuActionsForTest(actions)
        }
        actions.forEach {
            composeTestRule.onNodeWithTag(it.title).assertExists()
        }
    }

    @Test
    fun `test that when the same amount of buttons with icon as the max actions to show are added then show more button is not shown`() {
        val actions = List(defaultAmount) {
            MenuActionWithIconForTest("Action $it")
        }
        composeTestRule.setContent {
            MenuActionsForTest(actions)
        }
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
    }

    @Test
    fun `test that when more buttons with icon are added and count exceed max showable actions then only first max actions buttons are shown`() {
        val actions = List(defaultAmount + 1) {
            MenuActionWithIconForTest("Action $it")
        }
        composeTestRule.setContent {
            MenuActionsForTest(actions)
        }
        actions.take(defaultAmount).forEach {
            composeTestRule.onNodeWithTag(it.title).assertExists()
        }
        actions.drop(defaultAmount).forEach {
            composeTestRule.onNodeWithTag(it.title).assertDoesNotExist()
        }
    }

    @Test
    fun `test that when more buttons with icon are added than the max actions to show then show more button is shown`() {
        val actions = List(defaultAmount + 1) {
            MenuActionWithIconForTest("Action $it")
        }
        composeTestRule.setContent {
            MenuActionsForTest(actions)
        }
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertExists()
    }

    @Test
    fun `test that when an action without icon is added then show more button is shown`() {
        val actions = List(2) {
            MenuActionForTest("Action $it")
        }
        composeTestRule.setContent {
            MenuActionsForTest(actions)
        }
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertExists()
    }

    @Composable
    private fun MenuActionsForTest(actions: List<MenuAction>) =
        MenuActions(
            actions,
            defaultAmount,
            painterResource(id = R.drawable.ic_menu_more),
            Color.Black
        ) {}

    private class MenuActionWithIconForTest(val title: String) : MenuActionWithIcon {
        @Composable
        override fun getIconPainter() = painterResource(id = R.drawable.ic_menu_add)

        @Composable
        override fun getDescription() = title

    }

    private class MenuActionForTest(val title: String) : MenuAction {
        @Composable
        override fun getDescription() = title

    }

    private val defaultAmount = 3
}