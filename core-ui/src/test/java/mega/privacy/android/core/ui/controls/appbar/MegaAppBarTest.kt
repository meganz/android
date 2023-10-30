package mega.privacy.android.core.ui.controls.appbar

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionString
import mega.privacy.android.core.ui.model.MenuActionWithoutIcon
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MegaAppBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()


    private fun setTopBarScreen(
        title: String,
        badgeCount: Int? = null,
        appBarType: AppBarType = AppBarType.BACK_NAVIGATION,
        actions: List<MenuAction> = emptyList(),
    ) {
        composeTestRule.setContent {
            Scaffold(topBar = {
                MegaAppBar(
                    appBarType = appBarType,
                    title = title,
                    badgeCount = badgeCount,
                    actions = actions,
                )
            }) { padding ->
                Text(text = "Empty screen", modifier = Modifier.padding(padding))
            }
        }
    }

    private fun setTopBarScreenWithSubtitle(
        title: String,
        subtitle: String,
        badgeCount: Int? = null,
        appBarType: AppBarType = AppBarType.BACK_NAVIGATION,
        actions: List<MenuAction> = emptyList(),
    ) {
        composeTestRule.setContent {
            Scaffold(topBar = {
                MegaAppBar(
                    appBarType = appBarType,
                    title = title,
                    badgeCount = badgeCount,
                    actions = actions,
                    subtitle = subtitle
                )
            }) { padding ->
                Text(text = "Empty screen", modifier = Modifier.padding(padding))
            }
        }
    }

    @Test
    fun `test back arrow and title is displayed when app bar is shown`() {
        val title = "sample"
        setTopBarScreen(title = title)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithTag(APP_BAR_BACK_BUTTON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    fun `test subtitle is displayed when app bar is shown with subtitle`() {
        val title = "sample"
        val subtitle = "subtitle"
        setTopBarScreenWithSubtitle(title = title, subtitle = subtitle)
        composeTestRule.onNodeWithText(subtitle).assertIsDisplayed()
    }

    fun `test badge is displayed when app bar is shown with badge count`() {
        val title = "sample"
        val badgeCount = 33
        setTopBarScreen(title = title, badgeCount = badgeCount)
        composeTestRule.onNodeWithTag(APP_BAR_BADGE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(APP_BAR_BADGE).assertTextEquals(badgeCount.toString())
    }

    @Test
    fun `test back arrow is not displayed when app bar type is none`() {
        val title = "sample"
        setTopBarScreen(title = title, appBarType = AppBarType.NONE)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithTag(APP_BAR_BACK_BUTTON_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that icons are displayed when app bar is shown`() {
        val title = "sample"
        setTopBarScreen(title = title, actions = getSampleToolbarActions())
        composeTestRule.onNodeWithTag(menuButtonDownload).assertIsDisplayed()
        composeTestRule.onNodeWithTag(menuButtonDiscard).assertIsDisplayed()
        composeTestRule.onNodeWithTag(menuButtonCancel).assertIsDisplayed()
        composeTestRule.onNodeWithTag(menuButtonAlert).assertIsDisplayed()
    }

    @Test
    fun `test that maximum 4 icons are displayed when app bar is shown`() {
        val title = "sample"
        setTopBarScreen(title = title, actions = getMoreThanFiveToolbarActions())
        composeTestRule.onNodeWithTag(menuButtonDownload).assertIsDisplayed()
        composeTestRule.onNodeWithTag(menuButtonDiscard).assertIsDisplayed()
        composeTestRule.onNodeWithTag(menuButtonCancel).assertIsDisplayed()
        composeTestRule.onNodeWithTag(menuButtonAlert).assertIsDisplayed()
        composeTestRule.onNodeWithTag(menuButtonPwd)
            .assertDoesNotExist()
    }

    private fun getSampleToolbarActions(): List<MenuAction> {
        val item1 = object : MenuActionString(
            iconRes = R.drawable.ic_down,
            descriptionRes = R.string.action_long,
            testTag = menuButtonDownload
        ) {}
        val item2 = object : MenuActionString(
            iconRes = R.drawable.ic_menu,
            descriptionRes = R.string.discard,
            testTag = menuButtonDiscard
        ) {}
        val item3 = object :
            MenuActionString(
                iconRes = R.drawable.ic_back,
                descriptionRes = R.string.cancel_long,
                testTag = menuButtonCancel
            ) {}
        val item4 =
            object : MenuActionString(
                iconRes = R.drawable.ic_alert_circle,
                descriptionRes = R.string.action_long,
                testTag = menuButtonAlert
            ) {}
        val item5 = object : MenuActionWithoutIcon(
            descriptionRes = R.string.password_text,
            testTag = menuButtonPwd
        ) {}
        return listOf(item1, item2, item3, item4, item5)
    }

    private fun getMoreThanFiveToolbarActions(): List<MenuAction> {
        return getSampleToolbarActions().toMutableList().apply {
            add(object :
                MenuActionString(
                    iconRes = R.drawable.ic_favorite,
                    descriptionRes = R.string.dialog_title,
                    testTag = menuButtonFav
                ) {})
        }
    }

    val menuButtonCancel = "appbar:menu_button_cancel"
    val menuButtonDownload = "appbar:menu_button_download"
    val menuButtonDiscard = "appbar:menu_button_discard"
    val menuButtonAlert = "appbar:menu_button_alert"
    val menuButtonPwd = "appbar:menu_button_pwd"
    val menuButtonFav = "appbar:menu_button_fav"
}