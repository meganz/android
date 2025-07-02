package test.mega.privacy.android.app.presentation.achievements.view

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.app.presentation.achievements.view.AchievementView
import mega.privacy.android.app.presentation.achievements.view.AchievementViewTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import mega.privacy.android.app.fromId
import mega.privacy.android.domain.entity.achievement.AchievementType
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
internal class AchievementScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        currentStorage: Long? = 0,
        hasReferrals: Boolean = false,
        areAllRewardsExpired: Boolean = false,
        referralsStorage: Long? = 0,
        referralsAwardStorage: Long = 0,
        installAppStorage: Long? = 0,
        installAppAwardDaysLeft: Long? = 0,
        installAppAwardStorage: Long = 0,
        hasRegistrationAward: Boolean = false,
        registrationAwardDaysLeft: Long? = 0,
        registrationAwardStorage: Long = 0,
        installDesktopStorage: Long? = 0,
        installDesktopAwardDaysLeft: Long? = 0,
        installDesktopAwardStorage: Long = 0,
        hasMegaPassTrial: Boolean = false,
        megaPassTrialStorage: Long? = 0,
        megaPassTrialAwardStorage: Long = 0,
        hasMegaVPNTrial: Boolean = false,
        megaVPNTrialStorage: Long? = 0,
        megaVPNTrialAwardStorage: Long = 0,
        onInviteFriendsClicked: (Long) -> Unit = {},
        isAchievementEnabled: Boolean = false,
        onShowInfoAchievementsClicked: (achievementType: AchievementType) -> Unit = {},
        onReferBonusesClicked: () -> Unit = {},
        onMegaVPNFreeTrialClicked: (Boolean, Long, Long) -> Unit = {_, _, _ ->},
        onMegaPassFreeTrialClicked: (Boolean, Long, Long) -> Unit = {_, _, _ ->},
    ) {
        composeTestRule.setContent {
            AchievementView(
                currentStorage = currentStorage,
                hasReferrals = hasReferrals,
                areAllRewardsExpired = areAllRewardsExpired,
                referralsStorage = referralsStorage,
                referralsAwardStorage = referralsAwardStorage,
                installAppStorage = installAppStorage,
                installAppAwardDaysLeft = installAppAwardDaysLeft,
                installAppAwardStorage = installAppAwardStorage,
                hasRegistrationAward = hasRegistrationAward,
                registrationAwardDaysLeft = registrationAwardDaysLeft,
                registrationAwardStorage = registrationAwardStorage,
                installDesktopStorage = installDesktopStorage,
                installDesktopAwardDaysLeft = installDesktopAwardDaysLeft,
                installDesktopAwardStorage = installDesktopAwardStorage,
                hasMegaPassTrial = hasMegaPassTrial,
                megaPassTrialStorage = megaPassTrialStorage,
                megaPassTrialAwardStorage = megaPassTrialAwardStorage,
                hasMegaVPNTrial = hasMegaVPNTrial,
                megaVPNTrialStorage = megaVPNTrialStorage,
                megaVPNTrialAwardStorage = megaVPNTrialAwardStorage,
                onInviteFriendsClicked = onInviteFriendsClicked,
                isAchievementEnabled = isAchievementEnabled,
                onShowInfoAchievementsClicked = onShowInfoAchievementsClicked,
                onReferBonusesClicked = onReferBonusesClicked,
                onMegaVPNFreeTrialClicked = onMegaVPNFreeTrialClicked,
                onMegaPassFreeTrialClicked = onMegaPassFreeTrialClicked,
            )
        }
    }

    @Test
    fun `test that main achievement screen toolbar and title is visible with correct value`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(AchievementViewTestTags.HEADER)
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasText(fromId(R.string.unlocked_rewards_title))))
        composeTestRule.onNodeWithTag(AchievementViewTestTags.TOOLBAR)
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasText(fromId(sharedR.string.general_section_achievements))))
    }

    @Test
    fun `test that referrals reward is visible`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(AchievementViewTestTags.INVITE_BONUS_SECTION)
            .assert(hasText(fromId(R.string.title_referral_bonuses)))
            .assertIsDisplayed()
    }

    @Test
    fun `test that install app reward is visible`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(AchievementViewTestTags.INSTALL_MOBILE_SECTION)
            .assert(hasText(fromId(R.string.title_install_app)))
            .assertIsDisplayed()
    }

    @Test
    fun `test that install desktop reward is visible`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(AchievementViewTestTags.INSTALL_DESKTOP_SECTION)
            .assertIsDisplayed()
            .assert(hasText(fromId(R.string.title_install_desktop)))
    }

    @Test
    fun `test that registration reward is visible if user has it`() {
        setComposeContent(hasRegistrationAward = true)

        composeTestRule.onNodeWithTag(AchievementViewTestTags.REGISTRATION_BONUS_SECTION)
            .assert(hasText(fromId(R.string.title_regitration)))
    }

    @Test
    fun `test that registration reward is not visible if user does not have it`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(AchievementViewTestTags.REGISTRATION_BONUS_SECTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that free trial rewards are not visible`() {
        setComposeContent(
            isAchievementEnabled = true,
        )

        composeTestRule.onNodeWithTag(AchievementViewTestTags.START_MEGA_VPN_FREE_TRIAL_SECTION)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(AchievementViewTestTags.START_MEGA_PASS_FREE_TRIAL_SECTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that mega vpn trial reward is visible`() {
        val onMegaVPNFreeTrialClicked = mock<(Boolean, Long, Long) -> Unit>()
        setComposeContent(
            isAchievementEnabled = true,
            hasMegaVPNTrial = true,
            onMegaVPNFreeTrialClicked = onMegaVPNFreeTrialClicked
        )

        composeTestRule.onNodeWithTag(AchievementViewTestTags.START_MEGA_VPN_FREE_TRIAL_SECTION)
            .apply {
                assertIsDisplayed()
                performClick()
            }

        verify(onMegaVPNFreeTrialClicked).invoke(false, 0, 0)
    }

    @Test
    fun `test that mega pass trial reward is visible`() {
        val onMegaPassFreeTrialClicked = mock<(Boolean, Long, Long) -> Unit>()
        setComposeContent(
            isAchievementEnabled = true,
            hasMegaPassTrial = true,
            onMegaPassFreeTrialClicked = onMegaPassFreeTrialClicked
        )

        composeTestRule.onNodeWithTag(AchievementViewTestTags.START_MEGA_PASS_FREE_TRIAL_SECTION)
            .apply {
                assertIsDisplayed()
                performClick()
            }

        verify(onMegaPassFreeTrialClicked).invoke(false, 0, 0)
    }
}
