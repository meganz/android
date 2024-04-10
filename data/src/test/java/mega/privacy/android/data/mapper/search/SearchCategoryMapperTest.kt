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

    @ParameterizedTest(name = "test {0} is mapped correctly to Search category")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(input: Int, expected: SearchCategory) {
        val actual = underTest(input)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(MegaApiAndroid.FILE_TYPE_DEFAULT, SearchCategory.ALL),
        Arguments.of(MegaApiAndroid.FILE_TYPE_AUDIO, SearchCategory.AUDIO),
        Arguments.of(MegaApiAndroid.FILE_TYPE_VIDEO, SearchCategory.VIDEO),
        Arguments.of(MegaApiAndroid.FILE_TYPE_ALL_DOCS, SearchCategory.ALL_DOCUMENTS),
        Arguments.of(MegaApiAndroid.FILE_TYPE_PHOTO, SearchCategory.IMAGES),
        Arguments.of(MegaApiAndroid.FILE_TYPE_PDF, SearchCategory.PDF),
        Arguments.of(MegaApiAndroid.FILE_TYPE_PRESENTATION, SearchCategory.PRESENTATION),
        Arguments.of(MegaApiAndroid.FILE_TYPE_SPREADSHEET, SearchCategory.SPREADSHEET),
        Arguments.of(MegaApiAndroid.FILE_TYPE_OTHERS, SearchCategory.OTHER),
        Arguments.of(MegaApiAndroid.FILE_TYPE_DOCUMENT, SearchCategory.DOCUMENTS)
    )
}