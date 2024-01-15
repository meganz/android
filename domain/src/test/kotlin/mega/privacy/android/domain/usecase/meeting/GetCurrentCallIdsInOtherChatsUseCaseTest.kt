package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class GetCurrentCallIdsInOtherChatsUseCaseTest {
    private lateinit var underTest: GetCurrentCallIdsInOtherChatsUseCase

    private val callRepository = mock<CallRepository>()

    private val chatId = 123L

    @BeforeEach
    internal fun setUp() {
        underTest = GetCurrentCallIdsInOtherChatsUseCase(callRepository = callRepository)
    }

    @Test
    fun `test that empty lists return empty`() = runTest {
        callRepository.stub {
            onBlocking { getCallHandleList(any()) }.thenReturn(emptyList())
        }

        assertThat(underTest(chatId)).isEqualTo(emptyList<Long>())
    }

    @Test
    fun `test that call in current chat is not returned`() = runTest {
        callRepository.stub {
            onBlocking { getCallHandleList(any()) }.thenReturn(listOf(chatId))
        }

        assertThat(underTest(chatId)).isEqualTo(emptyList<Long>())
    }

    @ParameterizedTest(name = "test that value found for status {0} is returned")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Initial", "UserNoPresent", "Connecting", "Joining", "InProgress"]
    )
    internal fun `test value returned`(type: ChatCallStatus) = runTest {
        val expected = listOf(1234L)
        callRepository.stub {
            onBlocking { getCallHandleList(any()) }.thenReturn(emptyList())
            onBlocking { getCallHandleList(type) }.thenReturn(expected)
        }

        assertThat(underTest(chatId)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "test that value found for status {0} is returned")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Initial", "UserNoPresent", "Connecting", "Joining", "InProgress"]
    )
    internal fun `test value returned with two calls and one is current chat`(type: ChatCallStatus) =
        runTest {
            val received = listOf(chatId, 1234L)
            val expected = listOf(1234L)
            callRepository.stub {
                onBlocking { getCallHandleList(any()) }.thenReturn(emptyList())
                onBlocking { getCallHandleList(type) }.thenReturn(received)
            }

            assertThat(underTest(chatId)).isEqualTo(expected)
        }

    @ParameterizedTest(name = "test that value found for status {0} is returned")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Initial", "UserNoPresent", "Connecting", "Joining", "InProgress"]
    )
    internal fun `test value returned with two calls`(type: ChatCallStatus) = runTest {
        val expected = listOf(1234L, 2345L)
        callRepository.stub {
            onBlocking { getCallHandleList(any()) }.thenReturn(emptyList())
            onBlocking { getCallHandleList(type) }.thenReturn(expected)
        }

        assertThat(underTest(chatId)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "test that value found for status {0} is returned")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Initial", "UserNoPresent", "Connecting", "Joining", "InProgress"]
    )
    internal fun `test value returned with three calls and one is current chat`(type: ChatCallStatus) =
        runTest {
            val received = listOf(chatId, 1234L, 2345L)
            val expected = listOf(1234L, 2345L)
            callRepository.stub {
                onBlocking { getCallHandleList(any()) }.thenReturn(emptyList())
                onBlocking { getCallHandleList(type) }.thenReturn(received)
            }

            assertThat(underTest(chatId)).isEqualTo(expected)
        }

    @ParameterizedTest(name = "test that value found for status {0} is returned")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Initial", "UserNoPresent", "Connecting", "Joining", "InProgress"]
    )
    internal fun `test value returned with three calls`(type: ChatCallStatus) = runTest {
        val expected = listOf(1234L, 23445L, 3456L)
        callRepository.stub {
            onBlocking { getCallHandleList(any()) }.thenReturn(emptyList())
            onBlocking { getCallHandleList(type) }.thenReturn(expected)
        }

        assertThat(underTest(chatId)).isEqualTo(expected)
    }
}