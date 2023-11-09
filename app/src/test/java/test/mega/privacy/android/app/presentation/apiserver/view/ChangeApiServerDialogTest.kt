package test.mega.privacy.android.app.presentation.apiserver.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.apiserver.model.ApiServerUIState
import mega.privacy.android.app.presentation.apiserver.view.ChangeApiServerDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChangeApiServerDialogTest {

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that change server is displayed`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.title_change_server))
            .assertIsDisplayed()
    }

    @Test
    fun `test that change server message is displayed`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.staging_api_url_text))
            .assertIsDisplayed()
    }

    @Test
    fun `test that production option is displayed`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.production_api_server))
            .assertIsDisplayed()
    }

    @Test
    fun `test that staging option is displayed`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.staging_api_server))
            .assertIsDisplayed()
    }

    @Test
    fun `test that staging 444 option is displayed`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.staging444_api_server))
            .assertIsDisplayed()
    }

    @Test
    fun `test that sandbox3 option is displayed`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sandbox3_api_server))
            .assertIsDisplayed()
    }

    @Test
    fun `test that cancel button is displayed`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.general_cancel))
            .assertIsDisplayed()
    }

    @Test
    fun `test that OK button is displayed`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.general_ok))
            .assertIsDisplayed()
    }

    private fun initComposeRuleContent() {
        composeTestRule.setContent {
            ChangeApiServerDialog(
                uiState = ApiServerUIState(),
                onOptionSelected = {},
                onDismissRequest = {},
                onConfirmRequest = {},
            )
        }
    }
}