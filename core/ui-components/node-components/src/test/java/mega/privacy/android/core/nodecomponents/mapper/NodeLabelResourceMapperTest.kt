package mega.privacy.android.core.nodecomponents.mapper

import com.google.common.truth.Truth
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.model.label.Label
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeLabelResourceMapperTest {
    private val underTest =
        NodeLabelResourceMapper()

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
            NodeLabel.RED, null,
            Label(
                label = NodeLabel.RED,
                labelColor = R.color.label_red,
                labelName = sharedR.string.label_red,
                isSelected = false
            )
        ),
        Arguments.of(
            NodeLabel.GREEN, NodeLabel.GREEN,
            Label(
                label = NodeLabel.GREEN,
                labelColor = R.color.label_green,
                labelName = sharedR.string.label_green,
                isSelected = true
            )
        ),
        Arguments.of(
            NodeLabel.ORANGE, NodeLabel.GREY,
            Label(
                label = NodeLabel.ORANGE,
                labelColor = R.color.label_orange,
                labelName = sharedR.string.label_orange,
                isSelected = false
            )
        ),
        Arguments.of(
            NodeLabel.YELLOW, null,
            Label(
                label = NodeLabel.YELLOW,
                labelColor = R.color.label_yellow,
                labelName = sharedR.string.label_yellow,
                isSelected = false
            )
        ),
        Arguments.of(
            NodeLabel.BLUE, NodeLabel.BLUE,
            Label(
                label = NodeLabel.BLUE,
                labelColor = R.color.label_blue,
                labelName = sharedR.string.label_blue,
                isSelected = true
            )
        ),
        Arguments.of(
            NodeLabel.PURPLE, null,
            Label(
                label = NodeLabel.PURPLE,
                labelColor = R.color.label_purple,
                labelName = sharedR.string.label_purple,
                isSelected = false
            )
        ),
        Arguments.of(
            NodeLabel.GREY, null,
            Label(
                label = NodeLabel.GREY,
                labelColor = R.color.label_grey,
                labelName = sharedR.string.label_grey,
                isSelected = false
            )
        )
    )
}