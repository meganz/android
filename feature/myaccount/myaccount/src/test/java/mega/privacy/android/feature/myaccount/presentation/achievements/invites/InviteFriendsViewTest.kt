package mega.privacy.android.feature.myaccount.presentation.achievements.invites

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.model.InviteFriendsUIState
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteConfirmationDialog
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsView
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsViewTestTags.DESCRIPTION
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsViewTestTags.DIALOG_BUTTON
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsViewTestTags.DIALOG_CONTAINER
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsViewTestTags.DIALOG_IMAGE_ICON
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsViewTestTags.DIALOG_SUBTITLE
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsViewTestTags.DIALOG_TITLE
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsViewTestTags.FOOTER
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsViewTestTags.HOW_IT_WORKS_DESCRIPTION
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsViewTestTags.HOW_IT_WORKS_TITLE
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsViewTestTags.IMAGE_MAIN
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsViewTestTags.INVITE_CONTACTS_BUTTON
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsViewTestTags.TOOLBAR
import mega.privacy.android.feature.myaccount.presentation.achievements.util.toUnitString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class InviteFriendsViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val oneHundredMbInBytes = 104857600L

    @Test
    fun `test that toolbar should render with correct title`() {
        composeTestRule.setContent {
            InviteFriendsView(
                modifier = Modifier,
                uiState = InviteFriendsUIState(oneHundredMbInBytes, 365),
                sentInvitesCount = null,
                onInviteClick = {},
                onConfirmationDismiss = {},
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeTestRule.onNodeWithTag(TOOLBAR)
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasText(context.getString(sharedR.string.title_referral_bonuses))))
    }

    @Test
    fun `test that invite friends view should render correctly on first load`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val durationInDays = 365
        val formattedSize = oneHundredMbInBytes.toUnitString(context)
        val expectedDescription =
            context.getString(
                sharedR.string.figures_storage_referrals_achievements_text,
                formattedSize,
                durationInDays
            )

        composeTestRule.setContent {
            InviteFriendsView(
                modifier = Modifier,
                uiState = InviteFriendsUIState(oneHundredMbInBytes, durationInDays),
                sentInvitesCount = null,
                onInviteClick = {},
                onConfirmationDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag(IMAGE_MAIN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DESCRIPTION)
            .assertIsDisplayed()
            .assert(hasText(expectedDescription))
        composeTestRule.onNodeWithTag(INVITE_CONTACTS_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(HOW_IT_WORKS_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(HOW_IT_WORKS_DESCRIPTION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FOOTER).assertIsDisplayed()
    }

    @Test
    fun `test that invite friends view should render correctly on first load when valid duration is permanent`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val durationInDays = 0
        val formattedSize = oneHundredMbInBytes.toUnitString(context)
        val expectedDescription =
            context.getString(
                sharedR.string.figures_storage_referrals_achievements_text_permanent,
                formattedSize
            )

        composeTestRule.setContent {
            InviteFriendsView(
                modifier = Modifier,
                uiState = InviteFriendsUIState(oneHundredMbInBytes, durationInDays),
                sentInvitesCount = null,
                onInviteClick = {},
                onConfirmationDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag(IMAGE_MAIN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DESCRIPTION)
            .assertIsDisplayed()
            .assert(hasText(expectedDescription))
        composeTestRule.onNodeWithTag(INVITE_CONTACTS_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(HOW_IT_WORKS_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(HOW_IT_WORKS_DESCRIPTION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FOOTER).assertIsDisplayed()
    }

    @Test
    fun `test that invite contacts button click invokes onInviteClick`() {
        var clicked = false
        composeTestRule.setContent {
            InviteFriendsView(
                modifier = Modifier,
                uiState = InviteFriendsUIState(oneHundredMbInBytes, 365),
                sentInvitesCount = null,
                onInviteClick = { clicked = true },
                onConfirmationDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag(INVITE_CONTACTS_BUTTON).performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that confirmation dialog is shown when sent invites count is not null`() {
        composeTestRule.setContent {
            InviteFriendsView(
                modifier = Modifier,
                uiState = InviteFriendsUIState(oneHundredMbInBytes, 365),
                sentInvitesCount = 1,
                onInviteClick = {},
                onConfirmationDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag(DIALOG_CONTAINER).assertIsDisplayed()
    }

    @Test
    fun `test that invite confirmation dialog should render with correct view`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val description = sharedR.string.invite_sent_text
        composeTestRule.setContent {
            InviteConfirmationDialog(
                isDialogVisible = true,
                description = description,
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithTag(DIALOG_IMAGE_ICON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DIALOG_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DIALOG_SUBTITLE)
            .assertIsDisplayed()
            .assert(hasText(context.getString(description)))
        composeTestRule.onNodeWithTag(DIALOG_BUTTON).assertIsDisplayed()
    }
}
