package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RenameDropdownMenuItem
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RenameToolbarMenuItemDropDownTest {

    private val underTest = RenameDropdownMenuItem()

    private val nodeOne = mock<TypedFolderNode>()
    private val nodeTwo = mock<TypedFolderNode>()
    private val multipleNodes = listOf(nodeOne, nodeTwo)

    @ParameterizedTest(name = "when selected nodes have nodeAccessPermission: {0}, noNodeInBackups : {1}, and selected nodes are {2} then visibility is {3}")
    @MethodSource("provideArguments")
    fun `test that rename dropdown item visibility is updated`(
        hasNodeAccessPermission: Boolean,
        noNodeInBackups: Boolean,
        selectedNodes: List<TypedNode>,
        expected: Boolean,
    ) = runTest {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = hasNodeAccessPermission,
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
        Arguments.of(false, false, emptyList<TypedFolderNode>(), false),
        Arguments.of(false, false, listOf(nodeOne), false),
        Arguments.of(false, false, multipleNodes, false),
        Arguments.of(true, false, listOf<TypedFolderNode>(), false),
        Arguments.of(true, false, listOf(nodeOne), false),
        Arguments.of(true, false, multipleNodes, false),
        Arguments.of(true, true, emptyList<TypedFolderNode>(), false),
        Arguments.of(true, true, listOf(nodeOne), true),
        Arguments.of(true, true, multipleNodes, false),
        Arguments.of(false, true, listOf<TypedFolderNode>(), false),
        Arguments.of(false, true, listOf(nodeOne), false),
        Arguments.of(false, true, multipleNodes, false),
    )
}