package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GetContactFromChatUseCaseTest {
    private val repository: ContactsRepository = mock()
    private val getContactFromEmail: GetContactFromEmail = mock()

    private val underTest by lazy(LazyThreadSafetyMode.NONE) {
        GetContactFromChatUseCase(
            repository, getContactFromEmail,
        )
    }

    @Test
    fun `test that when valid chat is given contact is returned`() = runTest {
        val testEmail = "test@gmail.com"
        val mockContact = mock<ContactItem> {
            on { email }.thenReturn(testEmail)
        }
        whenever(repository.getUserEmailFromChat(any())).thenReturn(testEmail)
        whenever(getContactFromEmail(testEmail, true)).thenReturn(mockContact)
        val contact = underTest(chatId = 123456789, true)
        verify(getContactFromEmail, times(1)).invoke(any(), any())
        assertEquals(testEmail, contact?.email)
    }

    @Test
    fun `test that when invalid chat is given nothing is returned`() = runTest {
        whenever(repository.getUserEmailFromChat(any())).thenReturn(null)
        val contact = underTest(chatId = -1L, skipCache = true)
        verify(getContactFromEmail, times(0)).invoke(any(), any())
        assertNull(contact?.email)
    }
}