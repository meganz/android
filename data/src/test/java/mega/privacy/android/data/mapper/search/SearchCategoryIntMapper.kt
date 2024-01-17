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

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(raw: Int, searchCategory: SearchCategory) {
        val actual = underTest(searchCategory)
        Truth.assertThat(actual).isEqualTo(raw)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(MegaApiAndroid.FILE_TYPE_DEFAULT, SearchCategory.ALL),
        Arguments.of(MegaApiAndroid.FILE_TYPE_AUDIO, SearchCategory.AUDIO),
        Arguments.of(MegaApiAndroid.FILE_TYPE_VIDEO, SearchCategory.VIDEO),
        Arguments.of(MegaApiAndroid.FILE_TYPE_ALL_DOCS, SearchCategory.ALL_DOCUMENTS),
        Arguments.of(MegaApiAndroid.FILE_TYPE_PHOTO, SearchCategory.IMAGES),
    )
}