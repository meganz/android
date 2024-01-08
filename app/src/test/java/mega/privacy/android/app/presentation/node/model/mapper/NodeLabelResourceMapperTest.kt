package mega.privacy.android.app.presentation.node.model.mapper

import com.google.common.truth.Truth
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.node.label.Label
import mega.privacy.android.domain.entity.NodeLabel
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeLabelResourceMapperTest {
    private val underTest = NodeLabelResourceMapper()

    @ParameterizedTest(name = "Test node label {0} and selected label {1}")
    @MethodSource("provideParams")
    fun `test that values mapped correctly`(
        nodeLabel: NodeLabel,
        selectedNodeLabel: NodeLabel?,
        expected: Label,
    ) {
        val actual = underTest(nodeLabel = nodeLabel, selectedLabel = selectedNodeLabel)
        Truth.assertThat(expected).isEqualTo(actual)
    }

    private fun provideParams() = Stream.of(
        Arguments.of(
            NodeLabel.RED, null, Label(
                label = NodeLabel.RED,
                labelColor = R.color.salmon_400_salmon_300,
                labelName = R.string.label_red,
                isSelected = false
            )
        ),
        Arguments.of(
            NodeLabel.GREEN, NodeLabel.GREEN, Label(
                label = NodeLabel.GREEN,
                labelColor = R.color.green_400_green_300,
                labelName = R.string.label_green,
                isSelected = true
            )
        ),
        Arguments.of(
            NodeLabel.ORANGE, NodeLabel.GREY, Label(
                label = NodeLabel.ORANGE,
                labelColor = R.color.orange_400_orange_300,
                labelName = R.string.label_orange,
                isSelected = false
            )
        ),
        Arguments.of(
            NodeLabel.YELLLOW, null, Label(
                label = NodeLabel.YELLLOW,
                labelColor = R.color.yellow_600_yellow_300,
                labelName = R.string.label_yellow,
                isSelected = false
            )
        ),
        Arguments.of(
            NodeLabel.BLUE, NodeLabel.BLUE, Label(
                label = NodeLabel.BLUE,
                labelColor = R.color.blue_300_blue_200,
                labelName = R.string.label_blue,
                isSelected = true
            )
        ),
        Arguments.of(
            NodeLabel.PURPLE, null, Label(
                label = NodeLabel.PURPLE,
                labelColor = R.color.purple_300_purple_200,
                labelName = R.string.label_purple,
                isSelected = false
            )
        ),
        Arguments.of(
            NodeLabel.GREY, null, Label(
                label = NodeLabel.GREY,
                labelColor = R.color.grey_300,
                labelName = R.string.label_grey,
                isSelected = false
            )
        )
    )
}