package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CheckIfIAmInThisMeetingUseCaseTest {
    private lateinit var underTest: CheckIfIAmInThisMeetingUseCase
    private val callRepository: CallRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = CheckIfIAmInThisMeetingUseCase(callRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(callRepository)
    }

    @ParameterizedTest(name = "and the chat call status is {0}")
    @EnumSource(value = ChatCallStatus::class, names = ["InProgress", "Joining", "Connecting"])
    fun `test that the user is in the meeting when the chat call is not null`(
        status: ChatCallStatus,
    ) = runTest {
        val chatId = 1L
        val call = mock<ChatCall> {
            on { this.status } doReturn status
        }
        whenever(callRepository.getChatCall(chatId)).thenReturn(call)
        Truth.assertThat(underTest(chatId)).isTrue()
    }

    @ParameterizedTest(name = "and the chat call status is {0}")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Destroyed", "TerminatingUserParticipation", "UserNoPresent"]
    )
    fun `test that the user is not in the meeting when the chat call is not null`(
        status: ChatCallStatus,
    ) = runTest {
        val chatId = 1L
        val call = mock<ChatCall> {
            on { this.status } doReturn status
        }
        whenever(callRepository.getChatCall(chatId)).thenReturn(call)
        Truth.assertThat(underTest(chatId)).isFalse()
    }

    @Test
    fun `test that the user is not in the meeting when the chat call is null`() = runTest {
        val chatId = 1L
        whenever(callRepository.getChatCall(chatId)).thenReturn(null)
        Truth.assertThat(underTest(chatId)).isFalse()
    }
}