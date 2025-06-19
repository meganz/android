package mega.privacy.android.feature.payment.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChooseAccountScreenTopBarTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that maybe later button is shown when not in upgrade account flow`() {
        setContent(isUpgradeAccount = false)
        composeRule.onNodeWithTag(TEST_TAG_MAYBE_LATER_BUTTON).assertExists().assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.choose_account_screen_maybe_later_button_text))
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that back button is shown when in upgrade account flow`() {
        setContent(isUpgradeAccount = true)
        composeRule.onNodeWithTag(TEST_TAG_BACK_BUTTON).assertExists().assertIsDisplayed()
    }

    private fun setContent(
        isUpgradeAccount: Boolean = false,
        maybeLaterClicked: () -> Unit = {},
        onBack: () -> Unit = {},
    ) = composeRule.setContent {
        ChooseAccountScreenTopBar(
            isUpgradeAccount = isUpgradeAccount,
            maybeLaterClicked = maybeLaterClicked,
            onBack = onBack
        )
    }
} 