package test.mega.privacy.android.app.presentation.fileinfo.model.mapper

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.app.presentation.fileinfo.model.mapper.NodeActionMapper
import mega.privacy.android.domain.entity.node.NodeAction
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NodeActionMapperTest {

    private val underTest = NodeActionMapper()

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(raw: NodeAction, expected: FileInfoMenuAction) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(*NodeAction.values().map {
        val expected = when (it) {
            NodeAction.Download -> FileInfoMenuAction.Download
            NodeAction.ShareFolder -> FileInfoMenuAction.ShareFolder
            NodeAction.GetLink -> FileInfoMenuAction.GetLink
            NodeAction.SendToChat -> FileInfoMenuAction.SendToChat
            NodeAction.ManageLink -> FileInfoMenuAction.ManageLink
            NodeAction.RemoveLink -> FileInfoMenuAction.RemoveLink
            NodeAction.DisputeTakedown -> FileInfoMenuAction.DisputeTakedown
            NodeAction.Rename -> FileInfoMenuAction.Rename
            NodeAction.Move -> FileInfoMenuAction.Move
            NodeAction.Copy -> FileInfoMenuAction.Copy
            NodeAction.MoveToRubbishBin -> FileInfoMenuAction.MoveToRubbishBin
            NodeAction.Leave -> FileInfoMenuAction.Leave
            NodeAction.Delete -> FileInfoMenuAction.Delete
        }
        Arguments.of(it, expected)
    }.toTypedArray())
}