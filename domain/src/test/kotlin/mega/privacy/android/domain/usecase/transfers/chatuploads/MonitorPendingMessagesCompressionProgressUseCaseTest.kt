package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorPendingMessagesCompressionProgressUseCaseTest {
    private lateinit var underTest: MonitorPendingMessagesCompressionProgressUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeAll
    fun setup() {
        underTest = MonitorPendingMessagesCompressionProgressUseCase(
            chatMessageRepository
        )
    }

    @BeforeEach
    fun resetMocks() = reset(chatMessageRepository)

    @Test
    fun `test that use case returns the correct flow from chat message repository`() = runTest {
        val expected = mock<Flow<Map<Long, Progress>>>()
        whenever(chatMessageRepository.monitorPendingMessagesCompressionProgress()) doReturn expected

        val actual = underTest()

        assertThat(actual).isEqualTo(expected)
    }
}