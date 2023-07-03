package test.mega.privacy.android.app.presentation.achievements.referral

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.achievements.referral.model.ReferralBonusesUIState
import mega.privacy.android.app.presentation.achievements.referral.view.ReferralBonusView
import mega.privacy.android.app.presentation.achievements.referral.view.TestTags
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.achievement.ReferralBonusAchievements
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import test.mega.privacy.android.app.fromId
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
    fun `test that toolbar should render with correct title`() {
        val achievements = listOf(createReferralBonusAchievements("Qwerty Uiop", null))

        composeTestRule.setContent {
            ReferralBonusView(uiState = ReferralBonusesUIState(awardedInviteAchievements = achievements))
        }

        composeTestRule.onNodeWithTag(TestTags.TOOLBAR)
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasText(fromId(R.string.title_referral_bonuses))))
    }

    @Test
    fun `test that on first load should render with correct state`() {
        val achievements = listOf(createReferralBonusAchievements("Qwerty Uiop", "Avatar URI"))

        composeTestRule.setContent {
            ReferralBonusView(uiState = ReferralBonusesUIState(awardedInviteAchievements = achievements))
        }

        achievements.forEachIndexed { index, _ ->
            composeTestRule.onNodeWithTag(TestTags.AVATAR_IMAGE + index).assertIsDisplayed()
            composeTestRule.onNodeWithTag(TestTags.TEXT_AVATAR + index).assertDoesNotExist()
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
    fun `test that text avatar should be visible when avatar uri is null`() {
        val achievements = listOf(createReferralBonusAchievements("Qwerty Uiop", null))

        composeTestRule.setContent {
            ReferralBonusView(uiState = ReferralBonusesUIState(awardedInviteAchievements = achievements))
        }

        achievements.forEachIndexed { index, _ ->
            composeTestRule.onNodeWithTag(TestTags.TEXT_AVATAR + index).assertIsDisplayed()
            composeTestRule.onNodeWithTag(TestTags.AVATAR_IMAGE + index).assertDoesNotExist()
        }
    }

    @Test
    fun `test that title should be replaced with email when referred name is null`() {
        val achievements = listOf(createReferralBonusAchievements(null, null))

        composeTestRule.setContent {
            ReferralBonusView(uiState = ReferralBonusesUIState(awardedInviteAchievements = achievements))
        }

        achievements.forEachIndexed { index, _ ->
            composeTestRule.onNodeWithTag(TestTags.TITLE + index).assertWithText(email)
        }
    }

    private fun createReferralBonusAchievements(
        name: String?,
        avatar: String?,
    ): ReferralBonusAchievements {
        return ReferralBonusAchievements(
            contact = ContactItem(
                handle = 1L,
                email = email,
                contactData = ContactData(fullName = name, alias = "KG", avatarUri = avatar),
                defaultAvatarColor = null,
                visibility = UserVisibility.Visible,
                timestamp = 0,
                areCredentialsVerified = true,
                status = UserStatus.Online
            ),
            expirationInDays = expirationInDays,
            awardId = 1,
            expirationTimestampInSeconds = 12312312,
            rewardedStorageInBytes = oneHundredMbInBytes,
            rewardedTransferInBytes = oneHundredMbInBytes,
            referredEmails = listOf(email)
        )
    }

    private fun SemanticsNodeInteraction.assertWithText(text: String) {
        this.assertIsDisplayed().assert(hasText(text))
    }
}