package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.CopyTypedNodeUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetOrCreateMyChatsFilesFolderIdUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAttachableNodeIdUseCaseTest {

    private lateinit var underTest: GetAttachableNodeIdUseCase

    private val copyTypedNodeUseCase = mock<CopyTypedNodeUseCase>()
    private val getOrCreateMyChatsFilesFolderIdUseCase = mock<GetOrCreateMyChatsFilesFolderIdUseCase>()
    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    internal fun setup() {
        underTest = GetAttachableNodeIdUseCase(
            copyTypedNodeUseCase,
            getOrCreateMyChatsFilesFolderIdUseCase,
            nodeRepository,
        )
    }

    @BeforeEach
    internal fun resetMocks() {
        reset(
            copyTypedNodeUseCase,
            getOrCreateMyChatsFilesFolderIdUseCase,
            nodeRepository,
        )
        commonStub()
    }

    private fun commonStub() = runTest {
        whenever(nodeRepository.getMyUserHandleBinary()).thenReturn(USER_ID)
        whenever(getOrCreateMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(MY_CHATS_FOLDER_ID))
    }

    @Test
    fun `test that node id is returned when the logged user is the owner`() = runTest {
        val expected = NodeId(12L)
        val fileNode = mock<TypedFileNode> {
            on { id } doReturn expected
        }

        whenever(nodeRepository.getOwnerNodeHandle(expected)).thenReturn(USER_ID)

        val actual = underTest(fileNode)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that node is copied when the logged user is not the owner`() = runTest {
        val nodeID = NodeId(12L)
        val fileNode = mock<TypedFileNode> {
            on { id } doReturn nodeID
        }
        val expected = NodeId(54L)

        whenever(nodeRepository.getOwnerNodeHandle(nodeID)).thenReturn(USER_ID + 5)
        whenever(copyTypedNodeUseCase(fileNode, NodeId(MY_CHATS_FOLDER_ID), null))
            .thenReturn(expected)

        val actual = underTest(fileNode)
        assertThat(actual).isEqualTo(expected)
    }
}

private const val USER_ID = 55L
private const val MY_CHATS_FOLDER_ID = 83445L