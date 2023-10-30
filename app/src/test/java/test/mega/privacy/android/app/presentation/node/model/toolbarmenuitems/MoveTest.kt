package test.mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.node.model.menuaction.MoveMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Move
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MoveTest {

    private val underTest = Move(MoveMenuAction())

    private val incomingNode = mock<TypedFolderNode> {
        on { isIncomingShare }.thenReturn(true)
    }
    private val notIncomingNode = mock<TypedFolderNode> {
        on { isIncomingShare }.thenReturn(false)
    }
    private val multipleNodes = setOf(incomingNode, notIncomingNode)

    @ParameterizedTest(name = "when are selected nodes in backups is {0} and selected nodes are {1}, then is move item visible is {2}")
    @MethodSource("provideArguments")
    fun `test that the move item visibility is adjusted`(
        noNodeInBackups: Boolean,
        selectedNodes: Set<TypedNode>,
        expected: Boolean,
    ) {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = selectedNodes,
            canBeMovedToTarget = false,
            noNodeInBackups = noNodeInBackups,
            noNodeTakenDown = false,
            allFileNodes = false,
            resultCount = 10
        )
        Truth.assertThat(result).isEqualTo(expected)
    }

    private fun provideArguments() = Stream.of(
        Arguments.of(false, setOf(incomingNode), false),
        Arguments.of(false, setOf(notIncomingNode), false),
        Arguments.of(false, multipleNodes, false),
        Arguments.of(true, setOf(incomingNode), false),
        Arguments.of(true, setOf(notIncomingNode), true),
        Arguments.of(true, multipleNodes, false),
    )
}