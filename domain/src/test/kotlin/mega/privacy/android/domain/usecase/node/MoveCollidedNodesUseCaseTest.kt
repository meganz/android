package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishBinUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoveCollidedNodesUseCaseTest {
    private lateinit var underTest: MoveCollidedNodesUseCase

    private val moveNodeUseCase: MoveNodeUseCase = mock()
    private val moveNodeToRubbishBinUseCase: MoveNodeToRubbishBinUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = MoveCollidedNodesUseCase(
            moveNodeUseCase = moveNodeUseCase,
            moveNodeToRubbishBinUseCase = moveNodeToRubbishBinUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(moveNodeUseCase, moveNodeToRubbishBinUseCase)
    }

    private fun collision(
        nodeHandle: Long = 1L,
        collisionHandle: Long = 3L,
        isFile: Boolean = false,
    ) = mock<NodeNameCollision.Default> {
        on { this.nodeHandle } doReturn nodeHandle
        on { this.collisionHandle } doReturn collisionHandle
        on { parentHandle } doReturn 2L
        on { renameName } doReturn "new name"
        on { this.isFile } doReturn isFile
    }

    @Test
    fun `test that invoke moves the node under its rename name when renaming`() = runTest {
        whenever(moveNodeUseCase(NodeId(1L), NodeId(2L), "new name")) doReturn NodeId(1L)

        val result = underTest(listOf(collision()), rename = true)

        assertThat(result.count).isEqualTo(1)
        assertThat(result.errorCount).isEqualTo(0)
        verifyNoInteractions(moveNodeToRubbishBinUseCase)
    }

    @Test
    fun `test that invoke sends the colliding file to the rubbish bin when replacing a file`() =
        runTest {
            whenever(moveNodeUseCase(NodeId(any()), NodeId(any()), eq(null))) doReturn NodeId(1L)

            val result = underTest(listOf(collision(isFile = true)), rename = false)

            assertThat(result.errorCount).isEqualTo(0)
            verify(moveNodeToRubbishBinUseCase).invoke(NodeId(3L))
        }

    @Test
    fun `test that invoke does nothing when the colliding node is the node itself`() = runTest {
        val result = underTest(
            listOf(collision(nodeHandle = 1L, collisionHandle = 1L, isFile = true)),
            rename = false,
        )

        assertThat(result.count).isEqualTo(1)
        assertThat(result.errorCount).isEqualTo(0)
        verifyNoInteractions(moveNodeToRubbishBinUseCase, moveNodeUseCase)
    }

    @Test
    fun `test that invoke counts an error when moving a node fails`() = runTest {
        whenever(moveNodeUseCase(NodeId(any()), NodeId(any()), anyOrNull()))
            .thenThrow(RuntimeException::class.java)

        val result = underTest(listOf(collision()), rename = true)

        assertThat(result.count).isEqualTo(1)
        assertThat(result.errorCount).isEqualTo(1)
    }

    @Test
    fun `test that invoke throws when moving a node hits a foreign node error`() = runTest {
        whenever(moveNodeUseCase(NodeId(any()), NodeId(any()), anyOrNull()))
            .thenThrow(ForeignNodeException::class.java)

        assertThrows<ForeignNodeException> { underTest(listOf(collision()), rename = true) }
    }

    @Test
    fun `test that invoke throws when moving a node runs out of quota`() = runTest {
        whenever(moveNodeUseCase(NodeId(any()), NodeId(any()), anyOrNull()))
            .thenAnswer { throw NotEnoughQuotaMegaException(1, "") }

        assertThrows<NotEnoughQuotaMegaException> { underTest(listOf(collision()), rename = true) }
    }

    @Test
    fun `test that invoke throws when moving a node exceeds the quota`() = runTest {
        whenever(moveNodeUseCase(NodeId(any()), NodeId(any()), anyOrNull()))
            .thenAnswer { throw QuotaExceededMegaException(1, "") }

        assertThrows<QuotaExceededMegaException> { underTest(listOf(collision()), rename = true) }
    }

    @Test
    fun `test that invoke throws when a collision is a chat collision`() = runTest {
        assertThrows<IllegalArgumentException> {
            underTest(listOf(collision(), mock<NodeNameCollision.Chat>()), rename = false)
        }
        verifyNoInteractions(moveNodeUseCase, moveNodeToRubbishBinUseCase)
    }

    @Test
    fun `test that invoke throws when a collision is a file link collision`() = runTest {
        assertThrows<IllegalArgumentException> {
            underTest(listOf(mock<NodeNameCollision.FileLink>()), rename = false)
        }
        verifyNoInteractions(moveNodeUseCase, moveNodeToRubbishBinUseCase)
    }

    @Test
    fun `test that invoke counts each node when some of them fail to move`() = runTest {
        whenever(moveNodeUseCase(NodeId(1L), NodeId(2L), "new name")) doReturn NodeId(1L)
        whenever(moveNodeUseCase(NodeId(4L), NodeId(2L), "new name"))
            .thenThrow(RuntimeException::class.java)

        val result = underTest(
            listOf(collision(nodeHandle = 1L), collision(nodeHandle = 4L)),
            rename = true,
        )

        assertThat(result.count).isEqualTo(2)
        assertThat(result.errorCount).isEqualTo(1)
    }

    @Test
    fun `test that invoke counts no error when every node is moved`() = runTest {
        whenever(moveNodeUseCase(NodeId(any()), NodeId(any()), anyOrNull())) doReturn NodeId(1L)

        val result = underTest(
            listOf(collision(nodeHandle = 1L), collision(nodeHandle = 4L)),
            rename = true,
        )

        assertThat(result.count).isEqualTo(2)
        assertThat(result.errorCount).isEqualTo(0)
    }
}
