package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.SelectAllMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.SelectAllToolbarMenuItem
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectAllToolbarMenuItemTest {

    private val underTest = SelectAllToolbarMenuItem(SelectAllMenuAction())

    private val oneFileNodeSelected = mock<TypedFolderNode> {
        on { isTakenDown }.thenReturn(false)
    }
    private val oneFolderNodeSelected = mock<TypedFolderNode>()
    private val multipleNodes = listOf(oneFileNodeSelected, oneFolderNodeSelected)

    @ParameterizedTest(name = "when selected nodes are {0} and result count is {1} then visibility is {2}")
    @MethodSource("provideArguments")
    fun `test that select all item visibility is updated`(
        selectedNodes: List<TypedNode>,
        resultCount: Int,
        expected: Boolean,
    ) = runTest {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = selectedNodes,
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = false,
            allFileNodes = false,
            resultCount = resultCount
        )
        Truth.assertThat(result).isEqualTo(expected)
    }

    private fun provideArguments() = Stream.of(
        Arguments.of(emptyList<TypedFolderNode>(), 10, true),
        Arguments.of(multipleNodes, 2, false)
    )
}