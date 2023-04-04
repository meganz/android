package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetContactFromEmailUseCaseTest {
    private val contactsRepository = mock<ContactsRepository>()
    private val underTest = GetContactFromEmailUseCase(contactsRepository)
    private val testEmail = "test@mega.nz"
    private val contactItem = mock<ContactItem> {
        on { email }.thenReturn("test@mega.nz")
        on { handle }.thenReturn(12345)
    }

    @Test
    fun `test that use case returns contact item if valid email is given`() = runTest {
        whenever(contactsRepository.getContactItemFromUserEmail(testEmail, true)).thenReturn(
            contactItem
        )
        val actual = underTest(testEmail, skipCache = true)
        val expected = 12345
        Truth.assertThat(actual?.handle).isEqualTo(expected)
    }

    @Test
    fun `test that use case returns null if invalid email is given`() = runTest {
        whenever(contactsRepository.getContactItemFromUserEmail("", true)).thenReturn(null)
        val actual = underTest(testEmail, skipCache = true)
        Truth.assertThat(actual).isNull()
    }
}