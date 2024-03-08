package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.meeting.chat.mapper.InviteMultipleUsersAsContactResultMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.InviteUserAsContactResultMapper
import mega.privacy.android.app.presentation.meeting.chat.model.InviteUserAsContactResult
import mega.privacy.android.app.presentation.meeting.chat.model.messages.InviteMultipleUsersAsContactResult
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.usecase.contact.InviteContactUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever


@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactAttachmentMessageViewModelTest {
    lateinit var underTest: ContactAttachmentMessageViewModel

    private val inviteContactUseCase = mock<InviteContactUseCase>()
    private val inviteUserAsContactResultOptionMapper =
        mock<InviteUserAsContactResultMapper>()
    private val inviteMultipleUsersAsContactResultMapper =
        mock<InviteMultipleUsersAsContactResultMapper>()

    @BeforeAll
    fun setUp() {
        underTest = ContactAttachmentMessageViewModel(
            inviteContactUseCase = inviteContactUseCase,
            inviteUserAsContactResultOptionMapper = inviteUserAsContactResultOptionMapper,
            inviteMultipleUsersAsContactResultMapper = inviteMultipleUsersAsContactResultMapper
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            inviteContactUseCase,
            inviteUserAsContactResultOptionMapper,
            inviteMultipleUsersAsContactResultMapper
        )
    }

    @Test
    fun `test that ContactInviteSent is returned when invite one new contact is sent successfully`() =
        runTest {
            val contactUserHandle = 1234L
            val contactEmail = "a@b.c"
            val contactMessage = mock<ContactAttachmentMessage> {
                on { this.contactHandle } doReturn contactUserHandle
                on { this.contactEmail } doReturn contactEmail
            }
            whenever(inviteContactUseCase(contactEmail, contactUserHandle, null)).thenReturn(
                InviteContactRequest.Sent
            )
            whenever(
                inviteUserAsContactResultOptionMapper(
                    InviteContactRequest.Sent,
                    contactEmail
                )
            ).thenReturn(
                InviteUserAsContactResult.ContactInviteSent
            )

            assertThat(underTest.inviteContact(contactMessage)).isInstanceOf(
                InviteUserAsContactResult.ContactInviteSent::class.java
            )
        }

    @Test
    fun `test that AllSent is returned when invite multi contacts is sent successfully`() =
        runTest {
            val message1 = mock<ContactAttachmentMessage> {
                on { this.contactHandle } doReturn 1L
                on { this.contactEmail } doReturn "1@b.c"
            }
            val message2 = mock<ContactAttachmentMessage> {
                on { this.contactHandle } doReturn 2L
                on { this.contactEmail } doReturn "1@b.c"
            }
            val messageSet = setOf(message1, message2)
            whenever(inviteContactUseCase(any(), any(), any())).thenReturn(
                InviteContactRequest.Sent
            )
            whenever(
                inviteMultipleUsersAsContactResultMapper(
                    any(),
                )
            ).thenReturn(
                InviteMultipleUsersAsContactResult.AllSent(messageSet.size)
            )

            assertThat(underTest.inviteMultipleContacts(messageSet)).isEqualTo(
                InviteMultipleUsersAsContactResult.AllSent(messageSet.size)
            )
        }
}