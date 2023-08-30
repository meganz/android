package mega.privacy.android.data.mapper.search

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.search.SearchCategory
import nz.mega.sdk.MegaApiAndroid
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchCategoryMapperTest {
    private val underTest: SearchCategoryMapper = SearchCategoryMapper()

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(SearchCategory::class)
    fun `test mapping is not null`(expected: SearchCategory) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: SearchCategory, raw: Int) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(SearchCategory.ALL, MegaApiAndroid.FILE_TYPE_DEFAULT),
        Arguments.of(SearchCategory.AUDIO, MegaApiAndroid.FILE_TYPE_AUDIO),
        Arguments.of(SearchCategory.VIDEO, MegaApiAndroid.FILE_TYPE_VIDEO),
        Arguments.of(SearchCategory.DOCUMENTS, MegaApiAndroid.FILE_TYPE_DOCUMENT),
        Arguments.of(SearchCategory.IMAGES, MegaApiAndroid.FILE_TYPE_PHOTO),
    )
}