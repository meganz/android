package test.mega.privacy.android.app.namecollision.node.model.toolbarmenuitems

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.node.model.menuaction.DownloadMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Download
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadTest {

    private val underTest = Download(DownloadMenuAction())

    private val oneFileNodeSelected = mock<TypedFolderNode> {
        on { isTakenDown }.thenReturn(false)
    }
    private val oneFolderNodeSelected = mock<TypedFolderNode> {

    }
    private val multipleNodes = setOf(oneFileNodeSelected, oneFolderNodeSelected)

    @ParameterizedTest(name = "when are selected nodes taken down is {0}, then is download item visible is {1}")
    @MethodSource("provideArguments")
    fun `test that the download item visibility is adjusted`(
        noNodeIsTakenDown: Boolean,
        expected: Boolean,
    ) {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = multipleNodes,
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = noNodeIsTakenDown,
            allFileNodes = false,
            resultCount = 10
        )
        Truth.assertThat(result).isEqualTo(expected)
    }

    private fun provideArguments() = Stream.of(
        Arguments.of(false, false),
        Arguments.of(true, true)
    )
}