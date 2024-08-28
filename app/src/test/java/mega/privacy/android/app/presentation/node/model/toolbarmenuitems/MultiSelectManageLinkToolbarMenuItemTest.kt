package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.MultiSelectManageLinkToolbarMenuItem
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultiSelectManageLinkToolbarMenuItemTest {

    private val underTest = MultiSelectManageLinkToolbarMenuItem()

    private val nodeWithExportedData = mock<TypedFolderNode> {
        on { exportedData }.thenReturn(
            ExportedData(
                publicLinkCreationTime = 123456L,
                publicLink = "link"
            )
        )
    }
    private val nodeWithoutExportedData = mock<TypedFolderNode> {
        on { exportedData }.thenReturn(null)
    }
    private val multipleNodes = listOf(nodeWithExportedData, nodeWithoutExportedData)

    @ParameterizedTest(name = "when is selected node taken down is {1} and has node access permission is {0} and selected nodes are {2}, then is multi select manager link item visible is {3}")
    @MethodSource("provideArguments")
    fun `test that the multi select manage link item visibility is adjusted`(
        hasNodeAccessPermission: Boolean,
        noNodeTakenDown: Boolean,
        selectedNodes: List<TypedNode>,
        expected: Boolean,
    ) = runTest {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = hasNodeAccessPermission,
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
        Arguments.of(false, false, emptyList<TypedFolderNode>(), false),
        Arguments.of(false, false, listOf(nodeWithExportedData), false),
        Arguments.of(false, false, listOf(nodeWithoutExportedData), false),
        Arguments.of(false, false, multipleNodes, false),
        Arguments.of(true, false, emptyList<TypedFolderNode>(), false),
        Arguments.of(true, false, listOf(nodeWithExportedData), false),
        Arguments.of(true, false, listOf(nodeWithoutExportedData), false),
        Arguments.of(true, false, multipleNodes, false),
        Arguments.of(true, true, emptyList<TypedFolderNode>(), false),
        Arguments.of(true, true, listOf(nodeWithExportedData), true),
        Arguments.of(true, true, listOf(nodeWithoutExportedData), false),
        Arguments.of(true, true, multipleNodes, true),
        Arguments.of(false, true, emptyList<TypedFolderNode>(), false),
        Arguments.of(false, true, listOf(nodeWithExportedData), false),
        Arguments.of(false, true, listOf(nodeWithoutExportedData), false),
        Arguments.of(false, true, multipleNodes, false),
    )
}