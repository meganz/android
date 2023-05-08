package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetNodesByHandlesUseCaseTest {
    lateinit var underTest: GetNodesByHandlesUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()
    private val addNodeType = mock<AddNodeType>()

    private val unTypeNodeOne = mock<FolderNode>()
    private val unTypeNodeTwo = mock<FileNode>()
    private val unTypedNodeList = listOf(unTypeNodeOne, unTypeNodeTwo)

    @Before
    fun setUp() {
        underTest = GetNodesByHandlesUseCase(mediaPlayerRepository, addNodeType)
    }

    @Test
    fun `test that the AddNodeType has been invoked`() =
        runTest {
            val handles: List<Long> = listOf(12345, 23456)
            whenever(mediaPlayerRepository.getNodesByHandles(handles)).thenReturn(
                unTypedNodeList
            )

            underTest(handles)

            verify(addNodeType, times(1)).invoke(unTypeNodeOne)
            verify(addNodeType, times(1)).invoke(unTypeNodeTwo)
        }
}