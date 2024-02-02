package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.filenode.GetOwnNodeUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetMyChatsFilesFolderIdUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeToAttachUseCaseTest {

    private val copyNodeUseCase: CopyNodeUseCase = mock()
    private val getOwnNodeUseCase: GetOwnNodeUseCase = mock()
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()

    private val underTest = GetNodeToAttachUseCase(
        copyNodeUseCase = copyNodeUseCase,
        getOwnNodeUseCase = getOwnNodeUseCase,
        getMyChatsFilesFolderIdUseCase = getMyChatsFilesFolderIdUseCase,
        getNodeByIdUseCase = getNodeByIdUseCase
    )

    private val defaultFileNode = mock<DefaultTypedFileNode> {
        whenever(it.id).thenReturn(NodeId(DEFAULT_HANDLE))
        whenever(it.name).thenReturn("test")
    }

    private val newFileNode = mock<DefaultTypedFileNode> {
        whenever(it.id).thenReturn(NodeId(COPIED_HANDLE))
    }

    @ParameterizedTest(name = "test provided node {0}, myNode {1}, new node {2} returns expected")
    @MethodSource("provideParams")
    fun `test that expected is actual with provided params`(
        inputNode: TypedFileNode,
        myNode: TypedNode?,
        newNode: TypedNode?,
        expected: TypedNode?,
    ) = runTest {
        whenever(getOwnNodeUseCase(inputNode)).thenReturn(myNode)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(CHAT_FOLDER_HANDLE))
        whenever(
            copyNodeUseCase(
                inputNode.id,
                NodeId(CHAT_FOLDER_HANDLE),
                inputNode.name
            )
        ).thenReturn(NodeId(COPIED_HANDLE))
        whenever(getNodeByIdUseCase(NodeId(COPIED_HANDLE))).thenReturn(newNode)
        whenever(getOwnNodeUseCase(newFileNode)).thenReturn(defaultFileNode)

        val actual = underTest(inputNode)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideParams() = Stream.of(
        Arguments.of(
            defaultFileNode,
            null,
            newFileNode,
            defaultFileNode
        ),
        Arguments.of(
            defaultFileNode,
            newFileNode,
            newFileNode,
            newFileNode
        ),
        Arguments.of(
            defaultFileNode,
            null,
            defaultFileNode,
            null
        )
    )

    @AfterEach
    fun resetMocks() {
        reset(
            copyNodeUseCase,
            getOwnNodeUseCase,
            getMyChatsFilesFolderIdUseCase,
            getNodeByIdUseCase
        )
    }

    companion object {
        const val DEFAULT_HANDLE = 1234L
        const val COPIED_HANDLE = 12L
        const val CHAT_FOLDER_HANDLE = 34L
    }
}