package test.mega.privacy.android.app.presentation.achievements.view

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.achievements.view.AchievementView
import mega.privacy.android.app.presentation.achievements.view.AchievementViewTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.fromId

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
internal class AchievementScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that main achievement screen toolbar and title is visible with correct value`() {
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

        composeTestRule.onNodeWithTag(AchievementViewTestTags.HEADER)
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasText(fromId(R.string.unlocked_rewards_title))))
        composeTestRule.onNodeWithTag(AchievementViewTestTags.TOOLBAR)
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasText(fromId(R.string.achievements_title))))
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

        composeTestRule.onNodeWithTag(AchievementViewTestTags.INVITE_BONUS_SECTION)
            .assert(hasText(fromId(R.string.title_referral_bonuses)))
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

        composeTestRule.onNodeWithTag(AchievementViewTestTags.INSTALL_MOBILE_SECTION)
            .assert(hasText(fromId(R.string.title_install_app)))
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

        composeTestRule.onNodeWithTag(AchievementViewTestTags.INSTALL_DESKTOP_SECTION)
            .assertIsDisplayed()
            .assert(hasText(fromId(R.string.title_install_desktop)))
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

        composeTestRule.onNodeWithTag(AchievementViewTestTags.REGISTRATION_BONUS_SECTION)
            .assert(hasText(fromId(R.string.title_regitration)))
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

        composeTestRule.onNodeWithTag(AchievementViewTestTags.REGISTRATION_BONUS_SECTION)
            .assertDoesNotExist()
    }
}
