package test.mega.privacy.android.app.namecollision.node.model.toolbarmenuitems

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.node.model.menuaction.TrashMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Trash
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrashTest {

    private val underTest = Trash(TrashMenuAction())

    private val oneFileNodeSelected = mock<TypedFileNode> {
        on { isIncomingShare }.thenReturn(false)
    }
    private val incomingShareFolderNode = mock<TypedFolderNode> {
        on { isIncomingShare }.thenReturn(true)
    }
    private val oneFolderNodeSelected = mock<TypedFolderNode> {
        on { isIncomingShare }.thenReturn(false)
    }
    private val multipleNodes = setOf(oneFileNodeSelected, oneFolderNodeSelected)

    @ParameterizedTest(name = "when selected nodes are in backups is {0} can be moved is {1} permission is {2} and nodes are {3} then visibility is {4}")
    @MethodSource("provideArguments")
    fun `test that trash item visibility is updated`(
        noNodeInBackups: Boolean,
        canBeMovedToTarget: Boolean,
        hasNodeAccessPermission: Boolean,
        selectedNodes: Set<TypedNode>,
        expected: Boolean,
    ) {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = hasNodeAccessPermission,
            selectedNodes = selectedNodes,
            canBeMovedToTarget = canBeMovedToTarget,
            noNodeInBackups = noNodeInBackups,
            noNodeTakenDown = false,
            allFileNodes = false,
            resultCount = 10
        )
        Truth.assertThat(result).isEqualTo(expected)
    }

    private fun provideArguments() = Stream.of(
        Arguments.of(false, false, false, multipleNodes, false),
        Arguments.of(
            false,
            false,
            false,
            setOf(oneFileNodeSelected, oneFolderNodeSelected, incomingShareFolderNode),
            false
        ),
        Arguments.of(true, false, false, multipleNodes, false),
        Arguments.of(
            true,
            false,
            false,
            setOf(oneFileNodeSelected, oneFolderNodeSelected, incomingShareFolderNode),
            false
        ),
        Arguments.of(true, true, false, multipleNodes, false),
        Arguments.of(
            true,
            true,
            false,
            setOf(oneFileNodeSelected, oneFolderNodeSelected, incomingShareFolderNode),
            false
        ),
        Arguments.of(true, true, true, multipleNodes, true),
        Arguments.of(
            true,
            true,
            true,
            setOf(oneFileNodeSelected, oneFolderNodeSelected, incomingShareFolderNode),
            false
        ),
    )
}