package test.mega.privacy.android.app.presentation.meeting.chat.view.message.contact

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.meeting.chat.view.message.contact.ContactMessageViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.contact.GetUserUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactUseCase
import mega.privacy.android.domain.usecase.contact.IsContactRequestSentUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ContactMessageViewModelTest {

    private lateinit var underTest: ContactMessageViewModel

    private val getContactFromEmailUseCase: GetContactFromEmailUseCase = mock()
    private val getUserUseCase = mock<GetUserUseCase>()
    private val isContactRequestSentUseCase = mock<IsContactRequestSentUseCase>()
    private val inviteContactUseCase = mock<InviteContactUseCase>()

    private val email = "email"
    private val userHandle = 1234567890L
    private val userId = UserId(userHandle)

    @BeforeAll
    fun setup() {
        initTestClass()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getContactFromEmailUseCase,
            getUserUseCase,
            isContactRequestSentUseCase,
            inviteContactUseCase
        )
    }

    @Test
    fun `test that loadContactInfo() returns correctly when load from cache`() = runTest {
        val contactItem = ContactItem(
            contactData = ContactData(
                fullName = "fullName",
                alias = "alias",
                avatarUri = null,
            ),
            status = UserChatStatus.Away,
            visibility = UserVisibility.Visible,
            handle = userHandle,
            email = email,
            defaultAvatarColor = null,
            timestamp = userHandle,
            areCredentialsVerified = true,
        )
        whenever(getContactFromEmailUseCase.invoke(email, false)).thenReturn(contactItem)
        Truth.assertThat(underTest.loadContactInfo(email)).isEqualTo(contactItem)
    }

    @Test
    fun `test that loadContactInfo() returns correctly when load from sdk`() = runTest {
        val cacheContactItem = ContactItem(
            contactData = ContactData(
                fullName = "",
                alias = "alias",
                avatarUri = null,
            ),
            status = UserChatStatus.Away,
            visibility = UserVisibility.Visible,
            handle = userHandle,
            email = email,
            defaultAvatarColor = null,
            timestamp = userHandle,
            areCredentialsVerified = true,
        )
        val newContactItem = ContactItem(
            contactData = ContactData(
                fullName = "fullName",
                alias = "alias",
                avatarUri = null,
            ),
            status = UserChatStatus.Away,
            visibility = UserVisibility.Visible,
            handle = userHandle,
            email = email,
            defaultAvatarColor = null,
            timestamp = userHandle,
            areCredentialsVerified = true,
        )
        whenever(getContactFromEmailUseCase.invoke(email, false)).thenReturn(cacheContactItem)
        whenever(getContactFromEmailUseCase.invoke(email, true)).thenReturn(newContactItem)
        Truth.assertThat(underTest.loadContactInfo(email)).isEqualTo(newContactItem)
    }

    @Test
    fun `test that check contact invokes correctly when user is my contact`() = runTest {
        val onContactClicked = mock<(String) -> Unit>()
        val onNonContactClicked = mock<() -> Unit>()
        val onNonContactAlreadyInvitedClicked = mock<() -> Unit>()
        val user = mock<User> {
            on { visibility } doReturn UserVisibility.Visible
            on { email } doReturn email
        }
        whenever(getUserUseCase.invoke(userId)).thenReturn(user)
        underTest.checkUser(
            userHandle,
            email,
            onContactClicked,
            onNonContactClicked,
            onNonContactAlreadyInvitedClicked
        )
        verify(getUserUseCase).invoke(userId)
        verify(onContactClicked).invoke(email)
        verifyNoInteractions(isContactRequestSentUseCase)
        verifyNoInteractions(onNonContactClicked)
        verifyNoInteractions(onNonContactAlreadyInvitedClicked)
    }

    @ParameterizedTest(name = " when has been ever my contact is {0} and already sent invitation is {1}")
    @ArgumentsSource(CheckContactArgumentsProvider::class)
    fun `test that check contact invokes correctly when user is not my contact and`(
        onceContact: Boolean,
        invitationSent: Boolean,
    ) = runTest {
        val onContactClicked = mock<(String) -> Unit>()
        val onNonContactClicked = mock<() -> Unit>()
        val onNonContactAlreadyInvitedClicked = mock<() -> Unit>()
        val user = mock<User> {
            on { visibility } doReturn UserVisibility.Hidden
            on { email } doReturn email
        }
        whenever(getUserUseCase.invoke(userId))
            .thenReturn(if (onceContact) user else null)
        whenever(isContactRequestSentUseCase.invoke(email)).thenReturn(invitationSent)
        underTest.checkUser(
            userHandle,
            email,
            onContactClicked,
            onNonContactClicked,
            onNonContactAlreadyInvitedClicked
        )
        verify(getUserUseCase).invoke(userId)
        verify(isContactRequestSentUseCase).invoke(email)
        verifyNoInteractions(onContactClicked)
        if (invitationSent) {
            verify(onNonContactAlreadyInvitedClicked).invoke()
            verifyNoInteractions(onNonContactClicked)
        } else {
            verify(onNonContactClicked).invoke()
            verifyNoInteractions(onNonContactAlreadyInvitedClicked)
        }
    }

    @Test
    fun `test that invite user invokes correctly`() = runTest {
        val onInvitationSent = mock<() -> Unit>()
        whenever(inviteContactUseCase.invoke(email, userHandle, null))
            .thenReturn(mock())
        underTest.inviteUser(email, userHandle, onInvitationSent)
        verify(inviteContactUseCase).invoke(email, userHandle, null)
        verify(onInvitationSent).invoke()
    }

    private fun initTestClass() {
        underTest = ContactMessageViewModel(
            getContactFromEmailUseCase,
            getUserUseCase,
            isContactRequestSentUseCase,
            inviteContactUseCase
        )
    }

    internal class CheckContactArgumentsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext): Stream<out Arguments>? {
            return Stream.of(
                Arguments.of(true, true),
                Arguments.of(false, true),
                Arguments.of(true, false),
                Arguments.of(false, false),
            )
        }
    }
}