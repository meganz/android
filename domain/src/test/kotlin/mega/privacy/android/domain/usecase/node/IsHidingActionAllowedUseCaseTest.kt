package mega.privacy.android.domain.usecase.node

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.chat.GetMyChatsFilesFolderIdUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsHidingActionAllowedUseCaseTest {
    private lateinit var underTest: IsHidingActionAllowedUseCase

    private val getPrimarySyncHandleUseCase = mock<GetPrimarySyncHandleUseCase>()
    private val getSecondarySyncHandleUseCase = mock<GetSecondarySyncHandleUseCase>()
    private val getMyChatsFilesFolderIdUseCase = mock<GetMyChatsFilesFolderIdUseCase>()
    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = IsHidingActionAllowedUseCase(
            getPrimarySyncHandleUseCase = getPrimarySyncHandleUseCase,
            getSecondarySyncHandleUseCase = getSecondarySyncHandleUseCase,
            getMyChatsFilesFolderIdUseCase = getMyChatsFilesFolderIdUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getPrimarySyncHandleUseCase,
            getSecondarySyncHandleUseCase,
            getMyChatsFilesFolderIdUseCase,
            getRootNodeUseCase,
        )
    }

    @Test
    fun `node is not allowed to be hidden if it matches primary sync handle`() = runBlocking {
        val nodeId = NodeId(1)
        val rootNode = mock<Node> { on { id } doReturn NodeId(4) }
        whenever(getPrimarySyncHandleUseCase()).thenReturn(1L)
        whenever(getSecondarySyncHandleUseCase()).thenReturn(2L)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(3))
        whenever(getRootNodeUseCase()).thenReturn(rootNode)

        val result = underTest(nodeId)
        assertFalse(result)
    }

    @Test
    fun `node is not allowed to be hidden if it matches secondary sync handle`() = runBlocking {
        val nodeId = NodeId(2)
        val rootNode = mock<Node> { on { id } doReturn NodeId(4) }
        whenever(getPrimarySyncHandleUseCase()).thenReturn(1L)
        whenever(getSecondarySyncHandleUseCase()).thenReturn(2L)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(3))
        whenever(getRootNodeUseCase()).thenReturn(rootNode)

        val result = underTest(nodeId)
        assertFalse(result)
    }

    @Test
    fun `node is not allowed to be hidden if it matches my chats files folder id`() =
        runBlocking {
            val nodeId = NodeId(3)
            val rootNode = mock<Node> { on { id } doReturn NodeId(4) }
            whenever(getPrimarySyncHandleUseCase()).thenReturn(1L)
            whenever(getSecondarySyncHandleUseCase()).thenReturn(2L)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(3))
            whenever(getRootNodeUseCase()).thenReturn(rootNode)

            val result = underTest(nodeId)
            assertFalse(result)
        }

    @Test
    fun `node is not allowed to be hidden if it matches root node id`() = runBlocking {
        val nodeId = NodeId(4)
        val rootNode = mock<Node> { on { id } doReturn NodeId(4) }
        whenever(getPrimarySyncHandleUseCase()).thenReturn(1L)
        whenever(getSecondarySyncHandleUseCase()).thenReturn(2L)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(3))
        whenever(getRootNodeUseCase()).thenReturn(rootNode)

        val result = underTest(nodeId)
        assertFalse(result)
    }

    @Test
    fun `node is allowed to be hidden if it does not match any restricted id`() = runBlocking {
        val nodeId = NodeId(5)
        val rootNode = mock<Node> { on { id } doReturn NodeId(4) }
        whenever(getPrimarySyncHandleUseCase()).thenReturn(1L)
        whenever(getSecondarySyncHandleUseCase()).thenReturn(2L)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(3))
        whenever(getRootNodeUseCase()).thenReturn(rootNode)

        val result = underTest(nodeId)
        assertTrue(result)
    }
}