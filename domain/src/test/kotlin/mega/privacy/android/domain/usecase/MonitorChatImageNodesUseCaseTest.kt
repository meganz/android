package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedImageNode
import mega.privacy.android.domain.entity.node.chat.ChatImageFile
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorChatImageNodesUseCaseTest {
    private lateinit var underTest: MonitorChatImageNodesUseCase
    private val photosRepository = mock<PhotosRepository>()
    private val addImageTypeUseCase = mock<AddImageTypeUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorChatImageNodesUseCase(
            photosRepository = photosRepository,
            addImageTypeUseCase = addImageTypeUseCase,
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `invoke should return correct ImageNodes`() = runTest {
        val chatRoomId = 123L
        val messageIds = listOf(1L, 2L, 3L)
        val rawImageNodes = messageIds.map { messageId ->
            mock<ImageNode>().apply {
                whenever(id).thenReturn(NodeId(messageId))
            }
        }
        val rawTypedImageNodes = messageIds.map { messageId ->
            mock<TypedImageNode>().apply {
                whenever(id).thenReturn(NodeId(messageId))
            }
        }
        val chatImageFiles = rawTypedImageNodes.map { imageNode ->
            ChatImageFile(imageNode, chatRoomId, imageNode.id.longValue)
        }

        val expectedTypedImageNodes = messageIds.map { messageId ->
            mock<TypedImageNode>().apply {
                whenever(id).thenReturn(NodeId(messageId))
            }
        }
        messageIds.forEachIndexed { index, messageId ->
            whenever(photosRepository.getImageNodeFromChatMessage(chatRoomId, messageId))
                .thenReturn(rawImageNodes[index])
            whenever(addImageTypeUseCase(rawImageNodes[index]))
                .thenReturn(rawTypedImageNodes[index]) // Assuming addImageTypeUseCase returns the same node for simplicity
        }

        val result = underTest.invoke(chatRoomId, messageIds).first()

        verify(photosRepository, times(messageIds.size)).getImageNodeFromChatMessage(any(), any())
        verify(addImageTypeUseCase, times(messageIds.size)).invoke(any())
        assertEquals(chatImageFiles, result)
    }
}