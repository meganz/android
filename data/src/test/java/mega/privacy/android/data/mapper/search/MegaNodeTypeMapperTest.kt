package mega.privacy.android.data.mapper.search

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.search.NodeType
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaNodeTypeMapperTest {

    private val underTest: MegaNodeTypeMapper = MegaNodeTypeMapper()

    @ParameterizedTest(name = "when node type is {0}, then the mega node type value is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(input: NodeType, expected: Int) {
        val actual = underTest(input)
        Truth.assertThat(actual).isEqualTo(expected)
    }


    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(NodeType.FILE, MegaNode.TYPE_FILE),
        Arguments.of(NodeType.FOLDER, MegaNode.TYPE_FOLDER),
        Arguments.of(NodeType.UNKNOWN, MegaNode.TYPE_UNKNOWN),
    )
}