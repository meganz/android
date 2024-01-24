package mega.privacy.android.domain.usecase.chat.message.paging

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatHasMoreMessagesUseCaseTest {
    private lateinit var underTest: ChatHasMoreMessagesUseCase

    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    internal fun setUp() {
        underTest = ChatHasMoreMessagesUseCase(chatRepository = chatRepository)
    }

    @AfterEach
    internal fun tearDown() {
        reset(chatRepository)
    }

    @Test
    internal fun `test that true is returned if history is null`() = runTest {
        chatRepository.stub {
            onBlocking { getLastLoadResponse(any()) }.thenReturn(null)
        }

        assertThat(underTest(12L)).isTrue()
    }

    @Test
    internal fun `test that false is returned if the status is NONE`() = runTest {
        chatRepository.stub {
            onBlocking { getLastLoadResponse(any()) }.thenReturn(ChatHistoryLoadStatus.NONE)
        }

        assertThat(underTest(12L)).isFalse()
    }

    @ParameterizedTest(name = "ChatHistoryLoadStatus {0} should return true")
    @EnumSource(ChatHistoryLoadStatus::class, names = ["NONE"], mode = EnumSource.Mode.EXCLUDE)
    internal fun `test that true is returned for all other states`(state: ChatHistoryLoadStatus) =
        runTest {

        }
}