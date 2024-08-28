package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.ShareMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ShareToolBarMenuItem
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShareToolBarMenuItemTest {

    private val getLocalFilePathUseCase: GetLocalFilePathUseCase = mock()
    private val exportNodesUseCase: ExportNodeUseCase = mock()
    private val getFileUriUseCase: GetFileUriUseCase = mock()

    private val underTest = ShareToolBarMenuItem(
        menuAction = ShareMenuAction(),
        getLocalFilePathUseCase = getLocalFilePathUseCase,
        exportNodesUseCase = exportNodesUseCase,
        getFileUriUseCase = getFileUriUseCase
    )

    private val oneFileNodeSelected = mock<TypedFolderNode> {
        on { isTakenDown }.thenReturn(false)
    }
    private val oneFolderNodeSelected = mock<TypedFolderNode>()
    private val multipleNodes = listOf(oneFileNodeSelected, oneFolderNodeSelected)

    @ParameterizedTest(name = "when noNodeTakenDown: {0} and selected nodes are {1} then visibility is {2}")
    @MethodSource("provideArguments")
    fun `test that share item visibility is updated`(
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
        Arguments.of(false, multipleNodes, false),
        Arguments.of(true, emptyList<TypedFolderNode>(), false),
        Arguments.of(true, multipleNodes, true),
    )
}