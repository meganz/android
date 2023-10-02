package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsMeetingEndedUseCaseTest {
    private lateinit var underTest: IsMeetingEndedUseCase
    private val repository: ChatRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = IsMeetingEndedUseCase(repository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that the meeting has not ended when there are waiting room chat options`() = runTest {
        whenever(repository.hasWaitingRoomChatOptions(any())).thenReturn(true)
        Truth.assertThat(underTest(0, null)).isFalse()
    }

    @Test
    fun `test that the meeting has ended when there are no waiting room chat options`() =
        runTest {
            whenever(repository.hasWaitingRoomChatOptions(any())).thenReturn(false)
            Truth.assertThat(underTest(0, null)).isTrue()
            Truth.assertThat(underTest(0, emptyList())).isTrue()
        }

    @Test
    fun `test that the meeting has not ended when there are no waiting room chat options and the handles are not null or empty`() =
        runTest {
            whenever(repository.hasWaitingRoomChatOptions(any())).thenReturn(false)
            whenever(repository.getChatInvalidHandle()).thenReturn(-1L)
            Truth.assertThat(underTest(0, listOf(1L))).isFalse()
        }
}