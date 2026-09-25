package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InviteToChatUseCaseTest {

    lateinit var underTest: InviteToChatUseCase
    private val chatRepository: ChatRepository = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private val chatId = 1L

    @BeforeAll
    fun setup() {
        underTest = InviteToChatUseCase(chatRepository, ioDispatcher)
    }

    @BeforeEach
    fun reset() {
        reset(chatRepository)
    }

    @Test
    fun `test that one contact can be added successfully`() = runTest {
        val contactList = listOf("contact1")
        val handle = 2L

        whenever(chatRepository.getContactHandle(contactList[0])).thenReturn(handle)

        val result = underTest(chatId, contactList)

        assertThat(result.filter { it.isSuccess }.size).isEqualTo(contactList.size)
    }

    @Test
    fun `test that multiple contacts can be added successfully`() = runTest {
        val contactList = listOf("contact1", "contact2", "contact3")
        val handleList = listOf(2L, 3L, 4L)
        contactList.forEachIndexed { index, contact ->
            whenever(chatRepository.getContactHandle(contact)).thenReturn(handleList[index])
        }

        val result = underTest(chatId, contactList)
        assertThat(result.filter { it.isSuccess }.size).isEqualTo(contactList.size)
    }

    @Test
    fun `test that only successfully added contacts are returned`() = runTest {
        val contactIndexThatFailsToAdd = 1
        val contactList = listOf("contact1", "contact2", "contact3")
        val handleList = listOf(2L, 3L, 4L)
        contactList.forEachIndexed { index, contact ->
            whenever(chatRepository.getContactHandle(contact)).thenReturn(handleList[index])
        }
        whenever(
            chatRepository.inviteParticipantToChat(
                chatId,
                handleList[contactIndexThatFailsToAdd]
            )
        ).thenAnswer {
            throw MegaException(-1, "error")
        }

        val result = underTest(chatId, contactList)
        assertThat(result.filter { it.isSuccess }.size).isEqualTo(contactList.size - 1)
    }
}