package mega.privacy.android.feature.contact.list.view

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ContactItemContactInfoMenuItemEvent
import mega.privacy.mobile.analytics.event.ContactItemRemoveContactMenuItemEvent
import mega.privacy.mobile.analytics.event.ContactItemSendMessageMenuItemEvent
import mega.privacy.mobile.analytics.event.ContactItemStartCallMenuItemEvent
import mega.privacy.mobile.analytics.event.ContactItemStartVideoCallMenuItemEvent
import mega.privacy.mobile.analytics.event.RemoveContactConfirmButtonPressedEvent
import mega.privacy.mobile.analytics.event.RemoveContactConfirmationDialogEvent
import mega.privacy.mobile.analytics.event.RemoveContactDismissButtonPressedEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactActionsBottomSheetTest {

    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    @Test
    fun `test that tapping send message tracks ContactItemSendMessageMenuItemEvent`() {
        setSheet()

        composeTestRule.onNodeWithTag(CONTACT_ACTION_SEND_MESSAGE_TAG).performClick()

        assertThat(analyticsRule.events).contains(ContactItemSendMessageMenuItemEvent)
    }

    @Test
    fun `test that tapping audio call tracks ContactItemStartCallMenuItemEvent`() {
        setSheet()

        composeTestRule.onNodeWithTag(CONTACT_ACTION_AUDIO_CALL_TAG).performClick()

        assertThat(analyticsRule.events).contains(ContactItemStartCallMenuItemEvent)
    }

    @Test
    fun `test that tapping video call tracks ContactItemStartVideoCallMenuItemEvent`() {
        setSheet()

        composeTestRule.onNodeWithTag(CONTACT_ACTION_VIDEO_CALL_TAG).performClick()

        assertThat(analyticsRule.events).contains(ContactItemStartVideoCallMenuItemEvent)
    }

    @Test
    fun `test that tapping contact info tracks ContactItemContactInfoMenuItemEvent`() {
        setSheet()

        composeTestRule.onNodeWithTag(CONTACT_ACTION_CONTACT_INFO_TAG).performClick()

        assertThat(analyticsRule.events).contains(ContactItemContactInfoMenuItemEvent)
    }

    @Test
    fun `test that tapping remove contact tracks ContactItemRemoveContactMenuItemEvent`() {
        setSheet()

        composeTestRule.onNodeWithTag(CONTACT_ACTION_REMOVE_TAG).performClick()

        assertThat(analyticsRule.events).contains(ContactItemRemoveContactMenuItemEvent)
    }

    @Test
    fun `test that showing remove dialog tracks RemoveContactConfirmationDialogEvent`() {
        setRemoveDialog()

        assertThat(analyticsRule.events).contains(RemoveContactConfirmationDialogEvent)
    }

    @Test
    fun `test that confirming remove dialog tracks RemoveContactConfirmButtonPressedEvent and invokes onConfirm`() {
        var confirmed = false
        setRemoveDialog(onConfirm = { confirmed = true })

        composeTestRule.onNodeWithText(sharedR.string.general_remove).performClick()

        assertThat(analyticsRule.events).contains(RemoveContactConfirmButtonPressedEvent)
        assert(confirmed)
    }

    @Test
    fun `test that dismissing remove dialog tracks RemoveContactDismissButtonPressedEvent and invokes onDismiss`() {
        var dismissed = false
        setRemoveDialog(onDismiss = { dismissed = true })

        composeTestRule.onNodeWithText(sharedR.string.general_dismiss_dialog).performClick()

        assertThat(analyticsRule.events).contains(RemoveContactDismissButtonPressedEvent)
        assert(dismissed)
    }

    private fun setSheet() {
        composeTestRule.setContent {
            ContactActionsBottomSheet(
                contact = contact(),
                onDismiss = {},
                onSendMessage = {},
                onAudioCall = {},
                onVideoCall = {},
                onContactInfo = {},
                onRemove = {},
            )
        }
    }

    private fun setRemoveDialog(
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            RemoveContactDialog(
                displayName = "Alice",
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }
    }

    private fun contact(
        handle: Long = 1L,
        displayName: String = "Alice",
        email: String = "alice@test.com",
    ) = ContactItemUiState(
        handle = handle,
        displayName = displayName,
        status = ContactItemStatus.Online,
        lastSeen = null,
        avatar = AvatarData.Initials(
            initials = displayName.first().toString(),
            avatarColor = Color(0xFF2E7D32),
        ),
        isVerified = false,
        email = email,
    )

    private fun SemanticsNodeInteractionsProvider.onNodeWithText(@StringRes id: Int) =
        onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
        )
}
