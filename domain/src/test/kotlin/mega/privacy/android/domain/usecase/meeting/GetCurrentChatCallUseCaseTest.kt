package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class GetCurrentChatCallUseCaseTest {
    private lateinit var underTest: GetCurrentChatCallUseCase

    private val callRepository = mock<CallRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = GetCurrentChatCallUseCase(callRepository = callRepository)
    }

    @Test
    fun `test that empty lists return null`() = runTest {
        callRepository.stub {
            onBlocking { getCallHandleList(any()) }.thenReturn(emptyList())
        }

        assertThat(underTest()).isNull()
    }

    @ParameterizedTest(name = "test that value found for status {0} is returned")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Initial", "Connecting", "Joining", "InProgress"]
    )
    internal fun `test first value returned`(type: ChatCallStatus) = runTest {
        val expected = 1234L
        callRepository.stub {
            onBlocking { getCallHandleList(any()) }.thenReturn(emptyList())
            onBlocking { getCallHandleList(type) }.thenReturn(listOf(expected))
        }

        assertThat(underTest()).isEqualTo(expected)
    }
}