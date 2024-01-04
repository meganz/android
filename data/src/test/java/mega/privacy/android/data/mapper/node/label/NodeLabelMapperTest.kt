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
class NodeLabelMapperTest {
    private val underTest = NodeLabelMapper()

    @ParameterizedTest(name = "Test that {0} getting mapped correctly")
    @MethodSource("provideParams")
    fun `test that mapping done correctly`(
        label: Int,
        expected: NodeLabel?,
    ) {
        val actual = underTest(label)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParams() = Stream.of(
        Arguments.of(MegaNode.NODE_LBL_RED, NodeLabel.RED),
        Arguments.of(MegaNode.NODE_LBL_ORANGE, NodeLabel.ORANGE),
        Arguments.of(MegaNode.NODE_LBL_YELLOW, NodeLabel.YELLLOW),
        Arguments.of(MegaNode.NODE_LBL_GREEN, NodeLabel.GREEN),
        Arguments.of(MegaNode.NODE_LBL_PURPLE, NodeLabel.PURPLE),
        Arguments.of(MegaNode.NODE_LBL_BLUE, NodeLabel.BLUE),
        Arguments.of(MegaNode.NODE_LBL_GREY, NodeLabel.GREY),
        Arguments.of(99, null),
    )
}