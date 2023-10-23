package mega.privacy.android.core.ui.controls.appbar

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionString
import mega.privacy.android.core.ui.model.MenuActionWithoutIcon
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectModeAppBarTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val composeTestRule = createComposeRule()


    private fun setTopBarScreen(title: String, actions: List<MenuAction> = emptyList()) {
        composeTestRule.setContent {
            Scaffold(topBar = {
                SelectModeAppBar(title = title, actions = actions)
            }) { padding ->
                Text(text = "Empty screen", modifier = Modifier.padding(padding))
            }
        }
    }

    @Test
    fun `test back arrow and title is displayed when select mode app bar is shown`() {
        val title = "sample"
        setTopBarScreen(title = title)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SELECT_MODE_APP_BAR_BACK_BUTTON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that icons are displayed when select mode app bar is shown`() {
        val title = "sample"
        setTopBarScreen(title = title, actions = getSampleToolbarActions())
        composeTestRule.onNodeWithTag(context.getString(R.string.cancel)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(context.getString(R.string.discard)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(context.getString(R.string.cancel_long)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(context.getString(R.string.action_long)).assertIsDisplayed()
    }

    @Test
    fun `test that maximum 4 icons are displayed when select mode app bar is shown`() {
        val title = "sample"
        setTopBarScreen(title = title, actions = getMoreThanFiveToolbarActions())
        composeTestRule.onNodeWithTag(context.getString(R.string.cancel)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(context.getString(R.string.discard)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(context.getString(R.string.cancel_long)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(context.getString(R.string.action_long)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(context.getString(R.string.dialog_title))
            .assertDoesNotExist()
    }

    private fun getSampleToolbarActions(): List<MenuAction> {
        val item1 = object : MenuActionString(R.drawable.ic_down, R.string.cancel) {}
        val item2 = object : MenuActionString(R.drawable.ic_menu, R.string.discard) {}
        val item3 = object : MenuActionString(R.drawable.ic_chevron_up, R.string.cancel_long) {}
        val item4 = object : MenuActionString(R.drawable.ic_alert_circle, R.string.action_long) {}
        val item5 = object : MenuActionWithoutIcon(R.string.password_text) {}
        return listOf(item1, item2, item3, item4, item5)
    }

    private fun getMoreThanFiveToolbarActions(): List<MenuAction> {
        val item1 = object : MenuActionString(R.drawable.ic_down, R.string.cancel) {}
        val item2 = object : MenuActionString(R.drawable.ic_menu, R.string.discard) {}
        val item3 = object : MenuActionString(R.drawable.ic_chevron_up, R.string.cancel_long) {}
        val item4 = object : MenuActionString(R.drawable.ic_alert_circle, R.string.action_long) {}
        val item5 = object : MenuActionString(R.drawable.ic_favorite, R.string.dialog_title) {}
        val item6 = object : MenuActionWithoutIcon(R.string.password_text) {}
        return listOf(item1, item2, item3, item4, item5, item6)
    }

}