package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock


class GetMessageListUseCaseTest {
    private lateinit var underTest: GetMessageListUseCase

    internal fun initUseCase() {
        underTest = GetMessageListUseCase()
    }

    @Test
    internal fun `test that 32 items are returned if found`() = runTest {
        val mockMessage = mock<ChatMessage>()

        val expectedCount = 32
        val flow: Flow<ChatMessage?> = flow {
            repeat(expectedCount) {
                emit(
                    mockMessage
                )
            }
            awaitCancellation()
        }

        initUseCase()

        assertThat(underTest(flow)).hasSize(expectedCount)
    }

    @Test
    internal fun `test that fewer items are returned if null is returned`() = runTest {
        val mockMessage = mock<ChatMessage>()

        val expectedCount = 10
        val flow = flow {
            repeat(expectedCount) {
                emit(
                    mockMessage
                )
            }
            emit(null)
            awaitCancellation()
        }

        initUseCase()

        val actual = underTest(flow)

        assertThat(actual).hasSize(expectedCount)
    }

    @Test
    internal fun `test that unknown messages are filtered out`() = runTest {
        val mockMessage = mock<ChatMessage>()
        val invalidMessage = mock<ChatMessage> {
            on { type }.thenReturn(ChatMessageType.UNKNOWN)
        }

        val expectedCount = 12
        val flow = flow {
            repeat(expectedCount) {
                emit(
                    mockMessage
                )
            }
            repeat(20) {
                emit(
                    invalidMessage
                )
            }
            awaitCancellation()
        }

        initUseCase()

        assertThat(underTest(flow)).hasSize(expectedCount)
    }

    @Test
    internal fun `test that file revoked messages are filtered out`() = runTest {
        val mockMessage = mock<ChatMessage>()
        val invalidMessage = mock<ChatMessage> {
            on { type }.thenReturn(ChatMessageType.REVOKE_NODE_ATTACHMENT)
        }

        val expectedCount = 12
        val flow = flow {
            repeat(expectedCount) {
                emit(
                    mockMessage
                )
            }
            repeat(20) {
                emit(
                    invalidMessage
                )
            }
            awaitCancellation()
        }

        initUseCase()

        assertThat(underTest(flow)).hasSize(expectedCount)
    }

}