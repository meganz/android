package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.chat.ChatImageFile
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCachedOriginalPathUseCaseTest {
    private lateinit var underTest: GetCachedOriginalPathUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetCachedOriginalPathUseCase(
            chatMessageRepository,
        )
    }

    @BeforeEach
    fun resetMocks() = reset(
        chatMessageRepository,
    )

    @Test
    fun `test that getCachedOriginalPathForNode is called with the correct id`() {
        val nodeId = NodeId(1L)
        val chatFile = mock<ChatImageFile> {
            on { id } doReturn nodeId
        }
        underTest(chatFile)
        verify(chatMessageRepository).getCachedOriginalPathForNode(nodeId)
    }
}