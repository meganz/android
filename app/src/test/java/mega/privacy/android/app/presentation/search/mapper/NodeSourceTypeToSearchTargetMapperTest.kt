package mega.privacy.android.app.presentation.search.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.search.mapper.NodeSourceTypeToSearchTargetMapper
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.search.SearchTarget
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [NodeSourceTypeToSearchTargetMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NodeSourceTypeToSearchTargetMapperTest {
    private lateinit var underTest: NodeSourceTypeToSearchTargetMapper

    @BeforeAll
    fun setUp() {
        underTest = NodeSourceTypeToSearchTargetMapper()
    }

    @ParameterizedTest(name = "when node source type is {0}, then the search target is {1}")
    @MethodSource("provideParameters")
    fun `test that the correct search target is returned for a node source type`(
        nodeSourceType: NodeSourceType,
        searchTarget: SearchTarget,
    ) {
        assertThat(underTest(nodeSourceType)).isEqualTo(searchTarget)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(NodeSourceType.INCOMING_SHARES, SearchTarget.INCOMING_SHARE),
        Arguments.of(NodeSourceType.OUTGOING_SHARES, SearchTarget.OUTGOING_SHARE),
        Arguments.of(NodeSourceType.LINKS, SearchTarget.LINKS_SHARE),
        Arguments.of(NodeSourceType.HOME, SearchTarget.ROOT_NODES),
        Arguments.of(NodeSourceType.CLOUD_DRIVE, SearchTarget.ROOT_NODES),
        Arguments.of(NodeSourceType.RUBBISH_BIN, SearchTarget.ROOT_NODES),
        Arguments.of(NodeSourceType.BACKUPS, SearchTarget.ROOT_NODES),
        Arguments.of(NodeSourceType.OTHER, SearchTarget.ROOT_NODES),
    )
}
