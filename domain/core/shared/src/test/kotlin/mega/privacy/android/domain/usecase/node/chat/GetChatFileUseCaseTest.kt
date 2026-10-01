package mega.privacy.android.domain.usecase.node.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetChatFileUseCaseTest {
    private lateinit var underTest: GetChatFileUseCase

    private val nodeRepository = mock<NodeRepository>()
    private val addChatFileTypeUseCase = mock<AddChatFileTypeUseCase>()


    @BeforeAll
    fun setUp() {
        underTest = GetChatFileUseCase(nodeRepository, addChatFileTypeUseCase)
    }

    @BeforeEach
    fun resetMocks() = reset(
        nodeRepository,
        addChatFileTypeUseCase,
    )

    @Test
    fun `test that null is returned when there is no file node`() = runTest {
        whenever(nodeRepository.getNodeFromChatMessage(CHAT_ID, MESSAGE_ID, MESSAGE_INDEX))
            .thenReturn(null)
        assertThat(underTest(CHAT_ID, MESSAGE_ID, MESSAGE_INDEX)).isNull()
    }

    @Test
    fun `test that typed node returned by addChatFileTypeUseCase is returned`() = runTest {
        val fileNode = mock<FileNode>()
        val expected = mock<ChatDefaultFile>()
        whenever(nodeRepository.getNodeFromChatMessage(CHAT_ID, MESSAGE_ID, MESSAGE_INDEX))
            .thenReturn(fileNode)
        whenever(addChatFileTypeUseCase(fileNode, CHAT_ID, MESSAGE_ID, MESSAGE_INDEX))
            .thenReturn(expected)
        val actual = underTest(CHAT_ID, MESSAGE_ID, MESSAGE_INDEX)
        assertThat(actual).isEqualTo(expected)
    }
}

private const val CHAT_ID = 11L
private const val MESSAGE_ID = 11L
private const val MESSAGE_INDEX = 0