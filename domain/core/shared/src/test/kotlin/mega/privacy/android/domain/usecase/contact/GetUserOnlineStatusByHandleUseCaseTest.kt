package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetUserOnlineStatusByHandleUseCaseTest {
    private val contactsRepository = mock<ContactsRepository>()
    private val underTest = GetUserOnlineStatusByHandleUseCase(contactsRepository)
    private val testHandle = 12345L

    @Test
    fun `test that use case returns user status if valid handle is provided`() = runTest {
        whenever(contactsRepository.getUserOnlineStatusByHandle(testHandle)).thenReturn(UserChatStatus.Online)
        val actual = underTest(testHandle)
        val expected = UserChatStatus.Online
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that use case returns invalid if invalid handle is given`() = runTest {
        whenever(contactsRepository.getUserOnlineStatusByHandle(-1L)).thenReturn(UserChatStatus.Invalid)
        val actual = underTest(-1L)
        val expected = UserChatStatus.Invalid
        Truth.assertThat(actual).isEqualTo(expected)
    }
}