package mega.privacy.android.domain.usecase.mediaplayer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.AddNodeType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVideosBySearchTypeUseCaseTest {
    private lateinit var underTest: GetVideosBySearchTypeUseCase

    private val mediaPlayerRepository = mock<MediaPlayerRepository>()
    private val addNodeType = mock<AddNodeType>()

    private val unTypeNodeOne = mock<FileNode>()
    private val unTypeNodeTwo = mock<FileNode>()

    @BeforeAll
    fun initialise() {
        underTest =
            GetVideosBySearchTypeUseCase(
                mediaPlayerRepository = mediaPlayerRepository,
                addNodeType = addNodeType
            )
    }

    @BeforeEach
    fun resetMocks() {
        reset(mediaPlayerRepository, addNodeType, unTypeNodeOne, unTypeNodeTwo)
    }

    @Test
    fun `test that the AddNodeType has been invoked`() =
        runTest {
            val handle = 1234567L
            val sortOrder = SortOrder.ORDER_DEFAULT_ASC

            whenever(
                mediaPlayerRepository.getVideosBySearchType(
                    handle = handle,
                    searchString = "*",
                    recursive = true,
                    order = sortOrder
                )
            ).thenReturn(listOf(unTypeNodeOne, unTypeNodeTwo))

            underTest(handle = handle, searchString = "*", recursive = true, order = sortOrder)

            verify(addNodeType).invoke(unTypeNodeOne)
            verify(addNodeType).invoke(unTypeNodeTwo)
        }
}