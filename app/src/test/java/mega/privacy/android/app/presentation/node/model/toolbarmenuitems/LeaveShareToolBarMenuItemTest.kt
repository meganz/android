package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.LeaveShareMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.LeaveShareToolBarMenuItem
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LeaveShareToolBarMenuItemTest {

    private val underTest = LeaveShareToolBarMenuItem(
        menuAction = LeaveShareMenuAction(),
        stringWithDelimitersMapper = mock()
    )


    private val folder1 = mock<TypedFolderNode> {
        on { isIncomingShare }.thenReturn(true)
    }
    private val folder2 = mock<TypedFolderNode> {
        on { isIncomingShare }.thenReturn(true)
    }
    private val folderNotIncomingShare = mock<TypedFolderNode> {
        on { isIncomingShare }.thenReturn(false)
    }

    @ParameterizedTest(name = "when are selected nodes taken down is {0} and selected nodes are {1}, then is leave share item visible is {2}")
    @MethodSource("provideArguments")
    fun `test that the leave share item visibility is adjusted`(
        noNodeTakenDown: Boolean,
        selectedNodes: List<TypedNode>,
        expected: Boolean,
    ) = runTest {
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
        Arguments.of(false, emptyList<TypedFolderNode>(), false),
        Arguments.of(false, listOf(folder1), false),
        Arguments.of(false, listOf(folder1, folder2), false),
        Arguments.of(false, listOf(folderNotIncomingShare), false),
        Arguments.of(false, listOf(folder1, folder2, folderNotIncomingShare), false),
        Arguments.of(true, emptyList<TypedFolderNode>(), false),
        Arguments.of(true, listOf(folder1), true),
        Arguments.of(true, listOf(folder1, folder2), true),
        Arguments.of(true, listOf(folderNotIncomingShare), false),
        Arguments.of(true, listOf(folder1, folder2, folderNotIncomingShare), false)
    )
}