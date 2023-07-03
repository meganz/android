package test.mega.privacy.android.app.presentation.achievements.invites

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.toUnitString
import mega.privacy.android.app.main.InviteContactActivity
import mega.privacy.android.app.presentation.achievements.invites.model.InviteFriendsUIState
import mega.privacy.android.app.presentation.achievements.invites.view.InviteConfirmationDialog
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsView
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsViewTestTags.DESCRIPTION
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsViewTestTags.DIALOG_BUTTON
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsViewTestTags.DIALOG_IMAGE_ICON
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsViewTestTags.DIALOG_SUBTITLE
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsViewTestTags.DIALOG_TITLE
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsViewTestTags.FOOTER
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsViewTestTags.HOW_IT_WORKS_DESCRIPTION
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsViewTestTags.HOW_IT_WORKS_TITLE
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsViewTestTags.IMAGE_MAIN
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsViewTestTags.INVITE_CONTACTS_BUTTON
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsViewTestTags.TOOLBAR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowActivity
import test.mega.privacy.android.app.fromId

@HiltAndroidTest
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
                uiState = InviteFriendsUIState(oneHundredMbInBytes)
            )
        }

        composeTestRule.onNodeWithTag(TOOLBAR)
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasText(fromId(R.string.title_referral_bonuses))))
    }

    @Test
    fun `test that invite friends view should render correctly on first load`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val formattedSize = oneHundredMbInBytes.toUnitString(context)
        val expectedDescription =
            context.getString(R.string.figures_achievements_text_referrals, formattedSize)

        composeTestRule.setContent {
            InviteFriendsView(
                modifier = Modifier,
                uiState = InviteFriendsUIState(oneHundredMbInBytes)
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
    fun `test that should navigate to invite contacts activity when invite contacts button clicked`() {
        lateinit var context: Context

        composeTestRule.setContent {
            context = LocalContext.current
            InviteFriendsView(
                modifier = Modifier,
                uiState = InviteFriendsUIState(oneHundredMbInBytes)
            )
        }

        composeTestRule.onNodeWithTag(INVITE_CONTACTS_BUTTON).performClick()

        val shadowActivity: ShadowActivity = Shadow.extract(context as ComponentActivity)
        val nextStartedActivity = shadowActivity.nextStartedActivity

        assertThat(nextStartedActivity.component)
            .isEqualTo(ComponentName(context as Activity, InviteContactActivity::class.java))
    }

    @Test
    fun `test that invite confirmation dialog should render with correct view`() {
        val description = R.string.invite_contacts
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
            .assert(hasText(fromId(description)))
        composeTestRule.onNodeWithTag(DIALOG_BUTTON).assertIsDisplayed()
    }
}