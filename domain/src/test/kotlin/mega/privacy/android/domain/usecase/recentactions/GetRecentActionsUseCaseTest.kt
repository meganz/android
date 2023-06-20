package mega.privacy.android.domain.usecase.recentactions

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.RecentActionsRepository
import mega.privacy.android.domain.usecase.AddNodeType
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
class GetRecentActionsUseCaseTest {

    private lateinit var underTest: GetRecentActionsUseCase

    private val recentActionsRepository = mock<RecentActionsRepository>()
    private val addNodeType = mock<AddNodeType>()


    @BeforeAll
    fun setUp() {
        underTest = GetRecentActionsUseCase(
            recentActionsRepository = recentActionsRepository,
            addNodeType = addNodeType,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            recentActionsRepository,
            addNodeType,
        )
    }

    @Test
    fun `test that recentActionsRepository getRecentActions is invoked`() = runTest {
        whenever(recentActionsRepository.getRecentActions()).thenReturn(emptyList())
        underTest()
        verify(recentActionsRepository).getRecentActions()
    }

    @Test
    fun `test that for each recent action retrieved, add node type to each node inside the recent action`() =
        runTest {
            val nodes = (0..3).map {
                mock<FileNode>()
            }
            val list = listOf(
                RecentActionBucketUnTyped(
                    timestamp = 0L,
                    userEmail = "aaa@aaa.com",
                    parentHandle = 0L,
                    isUpdate = false,
                    isMedia = false,
                    nodes = listOf(nodes[0], nodes[1])
                ),
                RecentActionBucketUnTyped(
                    timestamp = 0L,
                    userEmail = "aaa@aaa.com",
                    parentHandle = 0L,
                    isUpdate = false,
                    isMedia = false,
                    nodes = listOf(nodes[2], nodes[3])
                ),
            )
            whenever(recentActionsRepository.getRecentActions()).thenReturn(list)

            underTest()

            (0..3).forEach {
                verify(addNodeType).invoke(nodes[it])
            }
        }

    @Test
    fun `test that after adding node type, only instance of TypedFileNode is returned`() =
        runTest {
            val node1 = mock<FileNode>()
            val node2 = mock<FolderNode>()

            val list = listOf(
                RecentActionBucketUnTyped(
                    timestamp = 0L,
                    userEmail = "aaa@aaa.com",
                    parentHandle = 0L,
                    isUpdate = false,
                    isMedia = false,
                    nodes = listOf(node1, node2)
                ),
            )
            whenever(recentActionsRepository.getRecentActions()).thenReturn(list)
            whenever(addNodeType(node1)).thenReturn(mock<TypedFileNode>())
            whenever(addNodeType(node2)).thenReturn(mock<TypedFolderNode>())

            val result = underTest()

            assertThat(result[0].nodes.size).isEqualTo(1)
        }
}
