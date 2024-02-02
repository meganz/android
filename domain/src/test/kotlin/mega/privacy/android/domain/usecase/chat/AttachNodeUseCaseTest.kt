package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttachNodeUseCaseTest {

    private val chatRepository = mock<ChatRepository>()

    private lateinit var underTest: AttachNodeUseCase

    @BeforeEach
    fun setup() {
        underTest = AttachNodeUseCase(chatRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that attach node use case invokes attachNode in repository`() = runTest {
        val chatId = 1L
        val nodeHandle = 2L
        underTest.invoke(chatId, NodeId(nodeHandle))
        verify(chatRepository).attachNode(chatId, nodeHandle)
        verifyNoMoreInteractions(chatRepository)
    }
}