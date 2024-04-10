package mega.privacy.android.data.mapper.search

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.search.SearchCategory
import nz.mega.sdk.MegaApiAndroid
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchCategoryIntMapperTest {
    private val underTest = SearchCategoryIntMapper()

    @ParameterizedTest(name = "test {0} is mapped correctly to integer {1}")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(searchCategory: SearchCategory, expected: Int) {
        val actual = underTest(searchCategory)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(SearchCategory.ALL, MegaApiAndroid.FILE_TYPE_DEFAULT),
        Arguments.of(SearchCategory.AUDIO, MegaApiAndroid.FILE_TYPE_AUDIO),
        Arguments.of(SearchCategory.DOCUMENTS, MegaApiAndroid.FILE_TYPE_DOCUMENT),
        Arguments.of(SearchCategory.IMAGES, MegaApiAndroid.FILE_TYPE_PHOTO),
        Arguments.of(SearchCategory.VIDEO, MegaApiAndroid.FILE_TYPE_VIDEO),
        Arguments.of(SearchCategory.OTHER, MegaApiAndroid.FILE_TYPE_OTHERS),
        Arguments.of(SearchCategory.PDF, MegaApiAndroid.FILE_TYPE_PDF),
        Arguments.of(SearchCategory.PRESENTATION, MegaApiAndroid.FILE_TYPE_PRESENTATION),
        Arguments.of(SearchCategory.SPREADSHEET, MegaApiAndroid.FILE_TYPE_SPREADSHEET),
    )
}