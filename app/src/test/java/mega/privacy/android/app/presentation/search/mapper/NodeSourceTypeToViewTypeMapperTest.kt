package mega.privacy.android.app.presentation.search.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.mapper.NodeSourceTypeToViewTypeMapper
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.node.NodeSourceType
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeSourceTypeToViewTypeMapperTest {
    val underTest = NodeSourceTypeToViewTypeMapper()

    @ParameterizedTest(name = "NodeSourceType {0} and constant {1}")
    @MethodSource("provideParam")
    fun `test that nodeSource type maps to expected type`(
        nodeSourceType: NodeSourceType,
        expected: Int?,
    ) {
        val actual = underTest(nodeSourceType)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideParam() = Stream.of(
        Arguments.of(NodeSourceType.HOME, NodeSourceTypeInt.FILE_BROWSER_ADAPTER),
        Arguments.of(NodeSourceType.CLOUD_DRIVE, NodeSourceTypeInt.FILE_BROWSER_ADAPTER),
        Arguments.of(NodeSourceType.BACKUPS, NodeSourceTypeInt.BACKUPS_ADAPTER),
        Arguments.of(NodeSourceType.LINKS, NodeSourceTypeInt.LINKS_ADAPTER),
        Arguments.of(NodeSourceType.OUTGOING_SHARES, NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER),
        Arguments.of(NodeSourceType.INCOMING_SHARES, NodeSourceTypeInt.INCOMING_SHARES_ADAPTER),
        Arguments.of(NodeSourceType.RUBBISH_BIN, NodeSourceTypeInt.RUBBISH_BIN_ADAPTER),
        Arguments.of(NodeSourceType.FAVOURITES, NodeSourceTypeInt.FAVOURITES_ADAPTER),
        Arguments.of(NodeSourceType.DOCUMENTS, NodeSourceTypeInt.DOCUMENTS_BROWSE_ADAPTER),
        Arguments.of(NodeSourceType.AUDIO, NodeSourceTypeInt.AUDIO_BROWSE_ADAPTER),
        Arguments.of(NodeSourceType.OTHER, null),
    )
}