package mega.privacy.android.core.achievementcomponents

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FreeTrailAchievementAwardedTextTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that text is displayed when not received award`() {
        val testText = "5 GB extra storage, valid for 30 days"
        composeTestRule.setContent {
            FreeTrailAchievementAwardedText(
                freeTrialText = testText,
                isReceivedAward = false,
                isExpired = false,
                isPermanent = false
            )
        }

        composeTestRule.onNodeWithText(testText)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that text is displayed when received award and not expired`() {
        val testText = "5 GB storage bonus applied, valid for 25 days"
        composeTestRule.setContent {
            FreeTrailAchievementAwardedText(
                freeTrialText = testText,
                isReceivedAward = true,
                isExpired = false,
                isPermanent = false
            )
        }

        composeTestRule.onNodeWithText(testText)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that text is displayed when received award and expired`() {
        val testText = "Expired"
        composeTestRule.setContent {
            FreeTrailAchievementAwardedText(
                freeTrialText = testText,
                isReceivedAward = true,
                isExpired = true,
                isPermanent = false
            )
        }

        composeTestRule.onNodeWithText(testText)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that text is displayed when permanent and not received`() {
        val testText = "5 GB extra storage, valid permanently"
        composeTestRule.setContent {
            FreeTrailAchievementAwardedText(
                freeTrialText = testText,
                isReceivedAward = false,
                isExpired = false,
                isPermanent = true
            )
        }

        composeTestRule.onNodeWithText(testText)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that text is displayed when permanent and received`() {
        val testText = "5 GB storage bonus applied, valid permanently"
        composeTestRule.setContent {
            FreeTrailAchievementAwardedText(
                freeTrialText = testText,
                isReceivedAward = true,
                isExpired = false,
                isPermanent = true
            )
        }

        composeTestRule.onNodeWithText(testText)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that component handles empty text`() {
        composeTestRule.setContent {
            FreeTrailAchievementAwardedText(
                freeTrialText = "",
                isReceivedAward = false,
                isExpired = false,
                isPermanent = false
            )
        }

        composeTestRule.onNodeWithText("").assertExists()
    }

    @Test
    fun `test that component handles long text`() {
        val longText = "This is a very long text that should still be displayed correctly " +
                "even when it spans multiple lines and contains a lot of information " +
                "about the achievement and its benefits"
        composeTestRule.setContent {
            FreeTrailAchievementAwardedText(
                freeTrialText = longText,
                isReceivedAward = true,
                isExpired = false,
                isPermanent = false
            )
        }

        composeTestRule.onNodeWithText(longText)
            .assertExists()
            .assertIsDisplayed()
    }
}
