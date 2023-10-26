package test.mega.privacy.android.app.upgradeAccount

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.view.FeatureRow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class FeatureRowTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that icon is displayed`() {
        composeRule.setContent {
            FeatureRow(
                drawableID = painterResource(id = R.drawable.ic_storage_onboarding_dialog),
                title = "Test title",
                description = "Test description",
                testTag = "test"
            )
        }

        composeRule.onNodeWithTag("test:icon").assertIsDisplayed()
    }

    @Test
    fun `test that title is displayed`() {
        composeRule.setContent {
            FeatureRow(
                drawableID = painterResource(id = R.drawable.ic_storage_onboarding_dialog),
                title = "Test title",
                description = "Test description",
                testTag = "test"
            )
        }

        composeRule.onNodeWithTag("test:title").assertIsDisplayed()
        composeRule.onNodeWithText("Test title").assertIsDisplayed()
    }

    @Test
    fun `test that description is displayed`() {
        composeRule.setContent {
            FeatureRow(
                drawableID = painterResource(id = R.drawable.ic_storage_onboarding_dialog),
                title = "Test title",
                description = "Test description",
                testTag = "test"
            )
        }

        composeRule.onNodeWithTag("test:description").assertIsDisplayed()
        composeRule.onNodeWithText("Test description").assertIsDisplayed()
    }
}