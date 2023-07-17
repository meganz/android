package test.mega.privacy.android.app.main.dialog.contactlink

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.main.dialog.contactlink.ContactLinkDialogFragment
import mega.privacy.android.app.main.dialog.contactlink.ContactLinkViewModel
import mega.privacy.android.domain.entity.contacts.ContactLink
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.usecase.contact.GetContactLinkUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ContactLinkViewModelTest {
    private lateinit var underTest: ContactLinkViewModel
    private val getContactLinkUseCase: GetContactLinkUseCase = mock()
    private val inviteContactUseCase: InviteContactUseCase = mock()
    private val savedStateHandle: SavedStateHandle = mock()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(getContactLinkUseCase, inviteContactUseCase, savedStateHandle)
    }

    private fun initTestClass() {
        underTest =
            ContactLinkViewModel(getContactLinkUseCase, inviteContactUseCase, savedStateHandle)
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
                inviteContactUseCase(
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
                inviteContactUseCase(
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