package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorPendingMessagesByStateUseCaseTest {
    private lateinit var underTest: MonitorPendingMessagesByStateUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = MonitorPendingMessagesByStateUseCase(
            chatMessageRepository = chatMessageRepository,
        )
    }

    @ParameterizedTest
    @EnumSource(PendingMessageState::class)
    fun `test that invoke calls repository method with correct parameter`(
        state: PendingMessageState,
    ) = runTest {
        val expected = mock<Flow<List<PendingMessage>>>()
        whenever(chatMessageRepository.monitorPendingMessagesByState(state)).thenReturn(expected)

        val actual = underTest(state)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that invoke calls repository method with correct parameters when there are multiple states`() =
        runTest {
            val states = arrayOf(PendingMessageState.PREPARING, PendingMessageState.READY_TO_UPLOAD)
            val expected = mock<Flow<List<PendingMessage>>>()
            whenever(chatMessageRepository.monitorPendingMessagesByState(states = states)).thenReturn(expected)

            val actual = underTest(states =states)

            assertThat(actual).isEqualTo(expected)
        }
}