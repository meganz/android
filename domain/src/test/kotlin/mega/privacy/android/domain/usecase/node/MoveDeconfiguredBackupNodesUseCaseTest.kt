package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test Class for [MoveDeconfiguredBackupNodesUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MoveDeconfiguredBackupNodesUseCaseTest {
    private lateinit var underTest: MoveDeconfiguredBackupNodesUseCase

    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MoveDeconfiguredBackupNodesUseCase(
            nodeRepository = nodeRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that nodeRepository moveOrRemoveDeconfiguredBackupNodes is invoked with correct parameter`() =
        runTest {
            val deconfiguredBackupRoot = NodeId(123L)
            val backupDestination = NodeId(456L)
            underTest(
                deconfiguredBackupRoot = deconfiguredBackupRoot,
                backupDestination = backupDestination,
            )
            verify(nodeRepository).moveOrRemoveDeconfiguredBackupNodes(
                deconfiguredBackupRoot = deconfiguredBackupRoot,
                backupDestination = backupDestination,
            )
        }
}