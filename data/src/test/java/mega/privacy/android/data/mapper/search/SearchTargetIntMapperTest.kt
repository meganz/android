package mega.privacy.android.data.mapper.search

import mega.privacy.android.domain.entity.search.SearchTarget
import nz.mega.sdk.MegaApiAndroid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchTargetIntMapperTest {

    private val underTest: SearchTargetIntMapper = SearchTargetIntMapper()

    @ParameterizedTest(name = "when search target is {0}, then the mega api android value is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(input: SearchTarget, expected: Int) {
        val actual = underTest(input)
        assertEquals(actual, expected)
    }


    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(SearchTarget.INCOMING_SHARE, MegaApiAndroid.SEARCH_TARGET_INSHARE),
        Arguments.of(SearchTarget.OUTGOING_SHARE, MegaApiAndroid.SEARCH_TARGET_OUTSHARE),
        Arguments.of(SearchTarget.LINKS_SHARE, MegaApiAndroid.SEARCH_TARGET_PUBLICLINK),
        Arguments.of(SearchTarget.ROOT_NODES, MegaApiAndroid.SEARCH_TARGET_ROOTNODE),
        Arguments.of(SearchTarget.ALL, MegaApiAndroid.SEARCH_TARGET_ALL),
    )
}