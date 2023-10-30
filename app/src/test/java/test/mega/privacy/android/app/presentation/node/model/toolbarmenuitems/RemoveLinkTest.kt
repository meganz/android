package test.mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveLink
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveLinkTest {

    private val underTest = RemoveLink()

    private val oneFileNodeSelected = mock<TypedFolderNode> {
        on { isTakenDown }.thenReturn(false)
    }
    private val oneFolderNodeSelected = mock<TypedFolderNode>()
    private val multipleNodes = setOf(oneFileNodeSelected, oneFolderNodeSelected)

    @ParameterizedTest(name = "when selected nodes are not taken down{0} and nodes are {1} then visibility is {2}")
    @MethodSource("provideArguments")
    fun `test that remove link item visibility is adjusted`(
        notTakenDown: Boolean,
        selectedNodes: Set<TypedNode>,
        expected: Boolean,
    ) {
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
        Arguments.of(false, emptySet<TypedFolderNode>(), false),
        Arguments.of(false, multipleNodes, false),
        Arguments.of(false, setOf(oneFileNodeSelected), false),
        Arguments.of(true, emptySet<TypedFolderNode>(), false),
        Arguments.of(true, multipleNodes, true),
        Arguments.of(true, setOf(oneFileNodeSelected), false)
    )
}