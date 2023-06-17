package test.mega.privacy.android.app.achievements.composables

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.main.megaachievements.composables.AchievementView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class AchievementScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that main achievement screen title is visible`() {
        composeTestRule.setContent {
            AchievementView(
                currentStorage = 0,
                hasReferrals = false,
                areAllRewardsExpired = false,
                referralsStorage = 0,
                referralsAwardStorage = 0,
                installAppStorage = 0,
                installAppAwardDaysLeft = 0,
                installAppAwardStorage = 0,
                hasRegistrationAward = false,
                registrationAwardDaysLeft = 0,
                registrationAwardStorage = 0,
                installDesktopStorage = 0,
                installDesktopAwardDaysLeft = 0,
                installDesktopAwardStorage = 0,
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.unlocked_rewards_title))
            .assertIsDisplayed()
    }

    @Test
    fun `test that referrals reward is visible`() {
        composeTestRule.setContent {
            AchievementView(
                currentStorage = 0,
                hasReferrals = false,
                areAllRewardsExpired = false,
                referralsStorage = 0,
                referralsAwardStorage = 0,
                installAppStorage = 0,
                installAppAwardDaysLeft = 0,
                installAppAwardStorage = 0,
                hasRegistrationAward = false,
                registrationAwardDaysLeft = 0,
                registrationAwardStorage = 0,
                installDesktopStorage = 0,
                installDesktopAwardDaysLeft = 0,
                installDesktopAwardStorage = 0,
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.title_referral_bonuses))
            .assertIsDisplayed()
    }

    @Test
    fun `test that install app reward is visible`() {
        composeTestRule.setContent {
            AchievementView(
                currentStorage = 0,
                hasReferrals = false,
                areAllRewardsExpired = false,
                referralsStorage = 0,
                referralsAwardStorage = 0,
                installAppStorage = 0,
                installAppAwardDaysLeft = 0,
                installAppAwardStorage = 0,
                hasRegistrationAward = false,
                registrationAwardDaysLeft = 0,
                registrationAwardStorage = 0,
                installDesktopStorage = 0,
                installDesktopAwardDaysLeft = 0,
                installDesktopAwardStorage = 0,
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.title_install_app))
            .assertIsDisplayed()
    }

    @Test
    fun `test that install desktop reward is visible`() {
        composeTestRule.setContent {
            AchievementView(
                currentStorage = 0,
                hasReferrals = false,
                areAllRewardsExpired = false,
                referralsStorage = 0,
                referralsAwardStorage = 0,
                installAppStorage = 0,
                installAppAwardDaysLeft = 0,
                installAppAwardStorage = 0,
                hasRegistrationAward = false,
                registrationAwardDaysLeft = 0,
                registrationAwardStorage = 0,
                installDesktopStorage = 0,
                installDesktopAwardDaysLeft = 0,
                installDesktopAwardStorage = 0,
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.title_install_desktop))
            .assertIsDisplayed()
    }

    @Test
    fun `test that registration reward is visible if user has it`() {
        composeTestRule.setContent {
            AchievementView(
                currentStorage = 0,
                hasReferrals = false,
                areAllRewardsExpired = false,
                referralsStorage = 0,
                referralsAwardStorage = 0,
                installAppStorage = 0,
                installAppAwardDaysLeft = 0,
                installAppAwardStorage = 0,
                hasRegistrationAward = true,
                registrationAwardDaysLeft = 0,
                registrationAwardStorage = 0,
                installDesktopStorage = 0,
                installDesktopAwardDaysLeft = 0,
                installDesktopAwardStorage = 0,
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.title_regitration))
            .assertIsDisplayed()
    }

    @Test
    fun `test that registration reward is not visible if user does not have it`() {
        composeTestRule.setContent {
            AchievementView(
                currentStorage = 0,
                hasReferrals = false,
                areAllRewardsExpired = false,
                referralsStorage = 0,
                referralsAwardStorage = 0,
                installAppStorage = 0,
                installAppAwardDaysLeft = 0,
                installAppAwardStorage = 0,
                hasRegistrationAward = false,
                registrationAwardDaysLeft = 0,
                registrationAwardStorage = 0,
                installDesktopStorage = 0,
                installDesktopAwardDaysLeft = 0,
                installDesktopAwardStorage = 0,
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.title_regitration))
            .assertDoesNotExist()
    }
}
