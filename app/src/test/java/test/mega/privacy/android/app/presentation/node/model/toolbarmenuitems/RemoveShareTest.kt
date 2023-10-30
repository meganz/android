package test.mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveShare
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveShareTest {

    private val underTest = RemoveShare()


    private val sharedFolder1 = mock<TypedFolderNode> {
        on { isPendingShare }.thenReturn(true)
    }
    private val sharedFolder2 = mock<TypedFolderNode> {
        on { isPendingShare }.thenReturn(true)
    }
    private val notSharedFolder = mock<TypedFolderNode> {
        on { isPendingShare }.thenReturn(false)
    }
    private val sharedFolders = setOf(sharedFolder1, sharedFolder2)
    private val mixedFoldersList = setOf(sharedFolder1, sharedFolder2, notSharedFolder)

    @ParameterizedTest(name = "when selected nodes are {0} then visibility is {1}")
    @MethodSource("provideArguments")
    fun `test that remove share item visibility is updated`(
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
        Arguments.of(sharedFolders, true),
        Arguments.of(mixedFoldersList, false)
    )
}