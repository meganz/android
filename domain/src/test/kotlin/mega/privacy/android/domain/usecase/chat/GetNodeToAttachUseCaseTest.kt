package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.chat.message.GetAttachableNodeIdUseCase
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

    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val getAttachableNodeIdUseCase: GetAttachableNodeIdUseCase = mock()

    private val underTest = GetNodeToAttachUseCase(
        getNodeByIdUseCase = getNodeByIdUseCase,
        getAttachableNodeIdUseCase = getAttachableNodeIdUseCase
    )

    private val defaultFileNode = mock<DefaultTypedFileNode> {
        whenever(it.id).thenReturn(NodeId(DEFAULT_HANDLE))
        whenever(it.name).thenReturn("test")
    }

    private val newFileNode = mock<DefaultTypedFileNode> {
        whenever(it.id).thenReturn(NodeId(COPIED_HANDLE))
    }

    @ParameterizedTest(name = "test provided node {0} returns expected {1}")
    @MethodSource("provideParams")
    fun `test that expected is actual with provided params`(
        inputNode: TypedFileNode,
        expected: TypedNode?,
    ) = runTest {
        whenever(getNodeByIdUseCase(NodeId(COPIED_HANDLE))).thenReturn(expected)
        whenever(getAttachableNodeIdUseCase(inputNode)).thenReturn(NodeId(COPIED_HANDLE))

        val actual = underTest(inputNode)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideParams() = Stream.of(
        Arguments.of(
            defaultFileNode,
            null,
        ),
        Arguments.of(
            defaultFileNode,
            newFileNode,
        ),
        Arguments.of(
            defaultFileNode,
            null,
        )
    )

    @AfterEach
    fun resetMocks() {
        reset(
            getAttachableNodeIdUseCase,
            getNodeByIdUseCase
        )
    }

    companion object {
        const val DEFAULT_HANDLE = 1234L
        const val COPIED_HANDLE = 12L
    }
}