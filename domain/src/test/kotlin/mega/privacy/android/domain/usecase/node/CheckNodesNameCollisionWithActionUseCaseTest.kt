package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.node.MultipleNodesRestoreResult
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckNodesNameCollisionWithActionUseCaseTest {
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase = mock()
    private val copyNodesUseCase: CopyNodesUseCase = mock()
    private val moveNodesUseCase: MoveNodesUseCase = mock()
    private val restoreNodesUseCase: RestoreNodesUseCase = mock()

    private lateinit var underTest: CheckNodesNameCollisionWithActionUseCase

    @BeforeAll
    fun setUp() {
        underTest = CheckNodesNameCollisionWithActionUseCase(
            checkNodesNameCollisionUseCase,
            copyNodesUseCase,
            moveNodesUseCase,
            restoreNodesUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            checkNodesNameCollisionUseCase,
            copyNodesUseCase,
            moveNodesUseCase,
            restoreNodesUseCase
        )
    }

    @Test
    fun `test that nodes are copied when non-conflict exists and collision type is COPY`() =
        runBlocking {
            val nodes = mapOf(1L to 2L)
            val noConflictNodes = mapOf(1L to 2L)
            whenever(checkNodesNameCollisionUseCase(nodes, NodeNameCollisionType.COPY)).thenReturn(
                NodeNameCollisionsResult(
                    noConflictNodes = noConflictNodes,
                    conflictNodes = emptyMap(),
                    type = NodeNameCollisionType.COPY
                )
            )

            underTest(nodes, NodeNameCollisionType.COPY)

            verify(copyNodesUseCase).invoke(noConflictNodes)
            verifyNoMoreInteractions(moveNodesUseCase, restoreNodesUseCase)
        }

    @Test
    fun `test that nodes are moved when non-conflict exists and collision type is MOVE`() =
        runBlocking {
            val nodes = mapOf(1L to 2L)
            val noConflictNodes = mapOf(1L to 2L)
            whenever(checkNodesNameCollisionUseCase(nodes, NodeNameCollisionType.MOVE)).thenReturn(
                NodeNameCollisionsResult(
                    noConflictNodes = noConflictNodes,
                    conflictNodes = emptyMap(),
                    type = NodeNameCollisionType.MOVE
                )
            )

            underTest(nodes, NodeNameCollisionType.MOVE)

            verify(moveNodesUseCase).invoke(noConflictNodes)
            verifyNoMoreInteractions(copyNodesUseCase, restoreNodesUseCase)
        }

    @Test
    fun `test that nodes are restored when non-conflict exists and collision type is RESTORE`() =
        runBlocking {
            val nodes = mapOf(1L to 2L)
            val noConflictNodes = mapOf(1L to 2L)
            whenever(
                checkNodesNameCollisionUseCase(
                    nodes,
                    NodeNameCollisionType.RESTORE
                )
            ).thenReturn(
                NodeNameCollisionsResult(
                    noConflictNodes = noConflictNodes,
                    conflictNodes = emptyMap(),
                    type = NodeNameCollisionType.RESTORE
                )
            )
            whenever(restoreNodesUseCase(noConflictNodes)).thenReturn(mock<MultipleNodesRestoreResult>())

            underTest(nodes, NodeNameCollisionType.RESTORE)

            verify(restoreNodesUseCase).invoke(noConflictNodes)
            verifyNoMoreInteractions(copyNodesUseCase, moveNodesUseCase)
        }

    @Test
    fun `test that movement result is set null when all nodes has name collision`() =
        runBlocking {
            val nodes = mapOf(1L to 2L)
            whenever(
                checkNodesNameCollisionUseCase(
                    nodes,
                    NodeNameCollisionType.COPY
                )
            ).thenReturn(
                NodeNameCollisionsResult(
                    noConflictNodes = emptyMap(),
                    conflictNodes = mapOf(1L to mock<NodeNameCollision.Default>()),
                    type = NodeNameCollisionType.COPY
                )
            )

            val result = underTest(nodes, NodeNameCollisionType.COPY)

            assertThat(result.moveRequestResult).isNull()
            verifyNoMoreInteractions(restoreNodesUseCase, copyNodesUseCase, moveNodesUseCase)
        }
}
