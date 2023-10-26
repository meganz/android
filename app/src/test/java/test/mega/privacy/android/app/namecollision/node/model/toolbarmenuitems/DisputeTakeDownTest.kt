package test.mega.privacy.android.app.namecollision.node.model.toolbarmenuitems

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.node.model.menuaction.DisputeTakeDownMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.DisputeTakeDown
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DisputeTakeDownTest {

    private val underTest = DisputeTakeDown(DisputeTakeDownMenuAction())
    private val takenDownNode = mock<TypedFolderNode> {
        on { isTakenDown }.thenReturn(false)
    }
    private val oneFileNodeSelected = mock<TypedFolderNode> {
        on { isTakenDown }.thenReturn(false)
    }
    private val oneFolderNodeSelected = mock<TypedFolderNode>()
    private val multipleNodes = setOf(oneFileNodeSelected, oneFolderNodeSelected)

    private val listWithTakenDownNode =
        setOf(oneFileNodeSelected, oneFolderNodeSelected, takenDownNode)

    @ParameterizedTest(name = "when are selected nodes taken down is {0} and selected nodes is {1}, then is dispute item visible is {2}")
    @MethodSource("provideArguments")
    fun `test that the dispute item visibility is adjusted`(
        noNodeTakenDown: Boolean,
        selectedNodes: Set<TypedNode>,
        expected: Boolean,
    ) {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = selectedNodes,
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = noNodeTakenDown,
            allFileNodes = false,
            resultCount = 10
        )
        Truth.assertThat(result).isEqualTo(expected)
    }

    private fun provideArguments() = Stream.of(
        Arguments.of(false, emptySet<TypedFolderNode>(), false),
        Arguments.of(false, setOf(oneFolderNodeSelected), true),
        Arguments.of(false, multipleNodes, false),
        Arguments.of(false, listWithTakenDownNode, false),
        Arguments.of(true, emptySet<TypedFolderNode>(), false),
        Arguments.of(true, setOf(oneFolderNodeSelected), false),
        Arguments.of(true, multipleNodes, false),
        Arguments.of(true, listWithTakenDownNode, false),
    )
}