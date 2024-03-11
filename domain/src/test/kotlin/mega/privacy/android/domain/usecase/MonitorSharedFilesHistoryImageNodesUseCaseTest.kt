package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.PhotosRepository
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
class MonitorSharedFilesHistoryImageNodesUseCaseTest {
    private lateinit var underTest: MonitorSharedFilesHistoryImageNodesUseCase
    private val photosRepository = mock<PhotosRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorSharedFilesHistoryImageNodesUseCase(
            photosRepository = photosRepository
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `invoke should return correct ImageNodes`() = runTest {
        val chatRoomId = 123L
        val messageIds = listOf(1L, 2L, 3L)
        val expectedImageNodes = messageIds.map { messageId ->
            mock<ImageNode>().apply {
                whenever(id).thenReturn(NodeId(messageId))
            }
        }

        messageIds.forEachIndexed { index, messageId ->
            whenever(photosRepository.getImageNodeFromChatMessage(chatRoomId, messageId))
                .thenReturn(expectedImageNodes[index])
        }

        val result = underTest.invoke(chatRoomId, messageIds).first()

        verify(photosRepository, times(messageIds.size)).getImageNodeFromChatMessage(any(), any())
        assertEquals(expectedImageNodes, result)
    }
}