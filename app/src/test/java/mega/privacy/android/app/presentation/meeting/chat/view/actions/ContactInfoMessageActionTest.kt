package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.mobile.analytics.event.ChatConversationViewContactsActionMenuItemEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.core.test.AnalyticsTestRule

@RunWith(AndroidJUnit4::class)
class ContactInfoMessageActionTest {
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private lateinit var underTest: ContactInfoMessageAction

    private val chatViewModel: ChatViewModel = mock()

    private val contactMessage = mock<ContactAttachmentMessage> {
        on { isContact } doReturn true
    }

    @Before
    fun setUp() {
        underTest = ContactInfoMessageAction(chatViewModel)
    }

    @Test
    fun `test that action applies to ContactAttachmentMessage which is my contact`() {
        assertThat(underTest.appliesTo(setOf(contactMessage))).isTrue()
    }

    @Test
    fun `test that action does not apply to ContactAttachmentMessage which is not contact`() {
        assertThat(underTest.appliesTo(setOf(mock<ContactAttachmentMessage> {
            on { isContact } doReturn false
        }))).isFalse()
    }

    @Test
    fun `test that action does not apply to other type of message`() {
        assertThat(underTest.appliesTo(setOf(mock<NormalMessage>()))).isFalse()
    }

    @Test
    fun `test that action does not apply to more than one message`() {
        assertThat(
            underTest.appliesTo(
                setOf(
                    mock<ContactAttachmentMessage>(),
                    mock<ContactAttachmentMessage>()
                )
            )
        ).isFalse()
    }

    @Test
    fun `test that composable contains bottom sheet option`() {
        val bottomSheetMenuItem = underTest.bottomSheetMenuItem(
            messages = setOf(mock<NormalMessage>()),
            hideBottomSheet = {},
            setAction = {},
        )
        composeTestRule.setContent { bottomSheetMenuItem() }
        composeTestRule.onNodeWithTag(underTest.bottomSheetItemTestTag).assertExists()
    }

    @Test
    fun `test that view model is invoked with onOpenContactInfo when action is clicked`() {
        val onHandled: () -> Unit = mock()
        val email = "email"
        val message = mock<ContactAttachmentMessage> {
            on { contactEmail } doReturn email
        }
        composeTestRule.setContent {
            underTest.OnTrigger(messages = setOf(message), onHandled = onHandled)
        }
        verify(chatViewModel).onOpenContactInfo(email)
    }

    @Test
    fun `test that onHandled() is invoked when action is clicked`() {
        val onHandled: () -> Unit = mock()
        composeTestRule.setContent {
            underTest.OnTrigger(messages = setOf(contactMessage), onHandled = onHandled)
        }
        verify(onHandled).invoke()
        assertThat(analyticsRule.events).contains(ChatConversationViewContactsActionMenuItemEvent)
    }
}