package mega.privacy.android.domain.usecase.node

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorFolderNodeDeleteUpdatesUseCaseTest {

    val nodeRepository: NodeRepository = mock()
    lateinit var underTest: MonitorFolderNodeDeleteUpdatesUseCase

    @BeforeAll
    fun setUp() {
        underTest = MonitorFolderNodeDeleteUpdatesUseCase(nodeRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            nodeRepository
        )
    }

    @Test
    fun `test that invoke should return correct deleted folder IDs`() = runTest {
        val folderNode1 = mock<FolderNode> {
            on { id } doReturn NodeId(1)
            on { isInRubbishBin } doReturn true
        }

        val folderNode2 = mock<FolderNode> {
            on { id } doReturn NodeId(2)
            on { isInRubbishBin } doReturn false
        }

        val folderNode3 = mock<FolderNode> {
            on { id } doReturn NodeId(3)
            on { isInRubbishBin } doReturn true
        }

        val nodeUpdateFlow = flow {
            emit(
                NodeUpdate(
                    mapOf(
                        folderNode1 to listOf(NodeChanges.Remove),
                        folderNode2 to emptyList(),
                        folderNode3 to listOf(NodeChanges.Remove)
                    )
                )
            )
        }

        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)

        underTest().test {
            assertThat(awaitItem()).hasSize(2)
            cancelAndConsumeRemainingEvents()
        }
    }
}
