package test.mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.node.model.menuaction.ClearSelectionMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ClearSelection
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearSelectionTest {

    private val underTest = ClearSelection(ClearSelectionMenuAction())

    private val oneFileNodeSelected = mock<TypedFolderNode> {
        on { isTakenDown }.thenReturn(false)
    }
    private val oneFolderNodeSelected = mock<TypedFolderNode>()
    private val multipleNodes = setOf(oneFileNodeSelected, oneFolderNodeSelected)

    @ParameterizedTest(name = "when selected nodes are {0}, then is clear selection item visible is {1}")
    @MethodSource("provideArguments")
    fun `test that the clear selection item visibility is adjusted`(
        selectedNodes: Set<TypedNode>,
        expected: Boolean,
    ) {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = selectedNodes,
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = false,
            allFileNodes = false,
            resultCount = 10
        )
        Truth.assertThat(result).isEqualTo(expected)
    }

    private fun provideArguments() = Stream.of(
        Arguments.of(emptySet<TypedFolderNode>(), false),
        Arguments.of(multipleNodes, true)
    )
}