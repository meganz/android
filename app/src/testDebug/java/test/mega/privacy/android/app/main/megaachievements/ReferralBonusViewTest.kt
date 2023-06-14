package test.mega.privacy.android.app.main.megaachievements

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.main.megaachievements.ReferralBonusView
import mega.privacy.android.app.main.megaachievements.ReferralBonusesUIState
import mega.privacy.android.app.main.megaachievements.TestTags
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.achievement.ReferralBonusAchievements
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReferralBonusViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val oneHundredMbInBytes = 104857600L
    private val name = "Qwerty Uiop"
    private val email = "qwerty@uiop.com"
    private val expirationInDays = Random.nextInt(from = 1, until = 200).toLong()

    @Test
    fun `test that on first load should render with correct state`() {
        val achievements = listOf(createReferralBonusAchievements("Qwerty Uiop"))

        composeTestRule.setContent {
            ReferralBonusView(uiState = ReferralBonusesUIState(awardedInviteAchievements = achievements))
        }

        achievements.forEachIndexed { index, _ ->
            composeTestRule.onNodeWithTag(TestTags.ROUNDED_IMAGE + index).assertIsDisplayed()
            composeTestRule.onNodeWithTag(TestTags.TITLE + index).assertWithText(name)
            composeTestRule.onNodeWithTag(TestTags.STORAGE + index)
                .assertWithText(
                    Util.getSizeString(
                        oneHundredMbInBytes,
                        ApplicationProvider.getApplicationContext()
                    )
                )
            composeTestRule.onNodeWithTag(TestTags.TRANSFER + index)
                .assertWithText(
                    Util.getSizeString(
                        oneHundredMbInBytes,
                        ApplicationProvider.getApplicationContext()
                    )
                )
            composeTestRule.onNodeWithTag(TestTags.DAYS_LEFT + index)
                .assertWithText("$expirationInDays d left")
        }
    }

    @Test
    fun `test that title should be replaced with email when referred name is null`() {
        val achievements = listOf(createReferralBonusAchievements(null))

        composeTestRule.setContent {
            ReferralBonusView(uiState = ReferralBonusesUIState(awardedInviteAchievements = achievements))
        }

        achievements.forEachIndexed { index, _ ->
            composeTestRule.onNodeWithTag(TestTags.TITLE + index).assertWithText(email)
        }
    }

    private fun createReferralBonusAchievements(name: String?): ReferralBonusAchievements {
        return ReferralBonusAchievements(
            referredAvatarUri = "",
            referredName = name,
            awardId = 1,
            expirationInDays = expirationInDays,
            rewardedStorageInBytes = oneHundredMbInBytes,
            rewardedTransferInBytes = oneHundredMbInBytes,
            referredEmails = listOf(email)
        )
    }

    private fun SemanticsNodeInteraction.assertWithText(text: String) {
        this.assertIsDisplayed().assert(hasText(text))
    }
}