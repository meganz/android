package mega.privacy.android.app.main.dialog.contactlink

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.main.dialog.contactlink.ContactLinkDialogFragment
import mega.privacy.android.app.main.dialog.contactlink.ContactLinkViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.ContactLink
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.usecase.contact.GetContactLinkUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ContactLinkViewModelTest {
    private lateinit var underTest: ContactLinkViewModel
    private val getContactLinkUseCase: GetContactLinkUseCase = mock()
    private val inviteContactWithHandleUseCase: InviteContactWithHandleUseCase = mock()
    private val savedStateHandle: SavedStateHandle = mock()

    @BeforeEach
    fun resetMocks() {
        reset(getContactLinkUseCase, inviteContactWithHandleUseCase, savedStateHandle)
    }

    private fun initTestClass() {
        underTest =
            ContactLinkViewModel(
                getContactLinkUseCase,
                inviteContactWithHandleUseCase,
                savedStateHandle
            )
    }

    @Test
    fun `test that contactLinkResult is updated correctly when getContactLinkUseCase return success`() =
        runTest {
            val contactLink = mock<ContactLink>()
            whenever(savedStateHandle.get<Long>(ContactLinkDialogFragment.EXTRA_USER_HANDLE))
                .thenReturn(1L)
            whenever(getContactLinkUseCase(1L)).thenReturn(contactLink)
            initTestClass()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.contactLinkResult?.isSuccess).isTrue()
                Truth.assertThat(state.contactLinkResult?.getOrNull()).isEqualTo(contactLink)
            }
        }

    @Test
    fun `test that contactLinkResult is updated correctly when getContactLinkUseCase throws exception`() =
        runTest {
            whenever(savedStateHandle.get<Long>(ContactLinkDialogFragment.EXTRA_USER_HANDLE))
                .thenReturn(1L)
            whenever(getContactLinkUseCase(1L)).thenAnswer { throw RuntimeException() }
            initTestClass()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.contactLinkResult?.isFailure).isTrue()
                Truth.assertThat(state.contactLinkResult?.exceptionOrNull())
                    .isInstanceOf(RuntimeException::class.java)
            }
        }

    @Test
    fun `test that sentInviteResult is updated correctly when inviteContactUseCase return success`() =
        runTest {
            val myEmail = "myEmail@mega.co.nz"
            val handle = 1L
            whenever(savedStateHandle.get<Long>(ContactLinkDialogFragment.EXTRA_USER_HANDLE))
                .thenReturn(1L)
            whenever(
                inviteContactWithHandleUseCase(
                    myEmail,
                    handle,
                    null
                )
            ).thenReturn(InviteContactRequest.Sent)
            initTestClass()
            underTest.state.test {
                Truth.assertThat(awaitItem().sentInviteResult).isNull()
                underTest.sendContactInvitation(handle, myEmail)
                val state = awaitItem()
                Truth.assertThat(state.sentInviteResult?.isSuccess).isTrue()
                Truth.assertThat(state.sentInviteResult?.getOrNull())
                    .isEqualTo(InviteContactRequest.Sent)
            }
        }

    @Test
    fun `test that sentInviteResult is updated correctly when inviteContactUseCase throws exception`() =
        runTest {
            val myEmail = "myEmail@mega.co.nz"
            val handle = 1L
            whenever(savedStateHandle.get<Long>(ContactLinkDialogFragment.EXTRA_USER_HANDLE))
                .thenReturn(1L)
            whenever(
                inviteContactWithHandleUseCase(
                    myEmail,
                    handle,
                    null
                )
            ).thenAnswer { throw RuntimeException() }
            initTestClass()
            underTest.state.test {
                Truth.assertThat(awaitItem().sentInviteResult).isNull()
                underTest.sendContactInvitation(handle, myEmail)
                val state = awaitItem()
                Truth.assertThat(state.sentInviteResult?.isFailure).isTrue()
                Truth.assertThat(state.sentInviteResult?.exceptionOrNull())
                    .isInstanceOf(RuntimeException::class.java)
            }
        }
}