package mega.privacy.android.data.mapper.node.label

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.NodeLabel
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeLabelIntMapperTest {

    private val underTest = NodeLabelIntMapper()

    @ParameterizedTest(name = "Test that {0} getting mapped correctly")
    @MethodSource("provideParams")
    fun `test that mapping is done correctly`(
        nodeLabel: NodeLabel,
        expected: Int,
    ) {
        val actual = underTest(nodeLabel)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParams() = Stream.of(
        Arguments.of(NodeLabel.RED, MegaNode.NODE_LBL_RED),
        Arguments.of(NodeLabel.ORANGE, MegaNode.NODE_LBL_ORANGE),
        Arguments.of(NodeLabel.YELLLOW, MegaNode.NODE_LBL_YELLOW),
        Arguments.of(NodeLabel.BLUE, MegaNode.NODE_LBL_BLUE),
        Arguments.of(NodeLabel.PURPLE, MegaNode.NODE_LBL_PURPLE),
        Arguments.of(NodeLabel.GREEN, MegaNode.NODE_LBL_GREEN),
        Arguments.of(NodeLabel.GREY, MegaNode.NODE_LBL_GREY),
    )
}