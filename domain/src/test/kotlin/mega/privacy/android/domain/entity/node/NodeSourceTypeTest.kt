package mega.privacy.android.domain.entity.node

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeSourceTypeTest {

    @ParameterizedTest
    @MethodSource("sharedItemSourceTypes")
    fun `test that isSharedItem returns true for shared item source types`(
        nodeSourceType: NodeSourceType,
    ) {
        assertThat(nodeSourceType.isSharedSource()).isTrue()
    }

    @ParameterizedTest
    @MethodSource("nonSharedItemSourceTypes")
    fun `test that isSharedItem returns false for non-shared item source types`(
        nodeSourceType: NodeSourceType,
    ) {
        assertThat(nodeSourceType.isSharedSource()).isFalse()
    }

    companion object {
        @JvmStatic
        fun sharedItemSourceTypes(): Stream<Arguments> = Stream.of(
            Arguments.of(NodeSourceType.INCOMING_SHARES),
            Arguments.of(NodeSourceType.OUTGOING_SHARES),
            Arguments.of(NodeSourceType.LINKS),
        )

        @JvmStatic
        fun nonSharedItemSourceTypes(): Stream<Arguments> = Stream.of(
            Arguments.of(NodeSourceType.HOME),
            Arguments.of(NodeSourceType.CLOUD_DRIVE),
            Arguments.of(NodeSourceType.RUBBISH_BIN),
            Arguments.of(NodeSourceType.BACKUPS),
            Arguments.of(NodeSourceType.FAVOURITES),
            Arguments.of(NodeSourceType.DOCUMENTS),
            Arguments.of(NodeSourceType.AUDIO),
            Arguments.of(NodeSourceType.OTHER),
            Arguments.of(NodeSourceType.OFFLINE),
        )
    }
}

