package mega.privacy.android.core.nodecomponents.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.node.NodeSourceType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class ViewTypeToNodeSourceTypeMapperTest {

    private val underTest = ViewTypeToNodeSourceTypeMapper()

    @ParameterizedTest(name = "viewType {0} maps to {1}")
    @MethodSource("provideViewTypeMappings")
    fun `test that view type maps to expected NodeSourceType`(
        viewType: Int?,
        expected: NodeSourceType,
    ) {
        assertThat(underTest(viewType)).isEqualTo(expected)
    }

    @Test
    fun `test that null view type defaults to CLOUD_DRIVE`() {
        assertThat(underTest(null)).isEqualTo(NodeSourceType.CLOUD_DRIVE)
    }

    @Test
    fun `test that unknown view type defaults to CLOUD_DRIVE`() {
        assertThat(underTest(9999)).isEqualTo(NodeSourceType.CLOUD_DRIVE)
    }

    companion object {
        @JvmStatic
        fun provideViewTypeMappings() = Stream.of(
            Arguments.of(NodeSourceTypeInt.FILE_BROWSER_ADAPTER, NodeSourceType.CLOUD_DRIVE),
            Arguments.of(NodeSourceTypeInt.RUBBISH_BIN_ADAPTER, NodeSourceType.RUBBISH_BIN),
            Arguments.of(NodeSourceTypeInt.LINKS_ADAPTER, NodeSourceType.LINKS),
            Arguments.of(NodeSourceTypeInt.INCOMING_SHARES_ADAPTER, NodeSourceType.INCOMING_SHARES),
            Arguments.of(NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER, NodeSourceType.OUTGOING_SHARES),
            Arguments.of(NodeSourceTypeInt.BACKUPS_ADAPTER, NodeSourceType.BACKUPS),
            Arguments.of(NodeSourceTypeInt.FAVOURITES_ADAPTER, NodeSourceType.FAVOURITES),
            Arguments.of(NodeSourceTypeInt.DOCUMENTS_BROWSE_ADAPTER, NodeSourceType.DOCUMENTS),
            Arguments.of(NodeSourceTypeInt.AUDIO_BROWSE_ADAPTER, NodeSourceType.AUDIO),
            Arguments.of(NodeSourceTypeInt.VIDEO_BROWSE_ADAPTER, NodeSourceType.VIDEOS),
            Arguments.of(NodeSourceTypeInt.SEARCH_BY_ADAPTER, NodeSourceType.SEARCH),
            Arguments.of(NodeSourceTypeInt.RECENTS_BUCKET_ADAPTER, NodeSourceType.RECENTS_BUCKET),
        )
    }
}
