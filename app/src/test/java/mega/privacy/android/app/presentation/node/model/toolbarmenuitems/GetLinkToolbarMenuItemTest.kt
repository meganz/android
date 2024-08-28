package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.GetLinkMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.GetLinkToolbarMenuItem
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetLinkToolbarMenuItemTest {

    private val underTest = GetLinkToolbarMenuItem(GetLinkMenuAction())

    private val oneFileNodeSelected = mock<TypedFolderNode> {
        on { isTakenDown }.thenReturn(false)
        on { exportedData }.thenReturn(null)
    }
    private val oneFolderNodeSelected = mock<TypedFolderNode>()
    private val multipleNodes = listOf(oneFileNodeSelected, oneFolderNodeSelected)

    @ParameterizedTest(name = "when are selected nodes taken down is {0} and selected nodes are {1}, then is get link item visible is {2}")
    @MethodSource("provideArguments")
    fun `test that the get link item visibility is adjusted`(
        notTakenDown: Boolean,
        selectedNodes: List<TypedNode>,
        expected: Boolean,
    ) = runTest {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = selectedNodes,
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = notTakenDown,
            allFileNodes = false,
            resultCount = 10
        )
        Truth.assertThat(result).isEqualTo(expected)
    }

    private fun provideArguments() = Stream.of(
        Arguments.of(false, emptyList<TypedFolderNode>(), false),
        Arguments.of(false, multipleNodes, false),
        Arguments.of(false, listOf(oneFileNodeSelected), false),
        Arguments.of(true, emptyList<TypedFolderNode>(), false),
        Arguments.of(true, multipleNodes, false),
        Arguments.of(true, listOf(oneFileNodeSelected), true)
    )
}