package mega.privacy.android.data.mapper.photos

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.SubCategory
import nz.mega.sdk.MegaNodeScopeFilter.FILE_SUBTYPE_GIF
import nz.mega.sdk.MegaNodeScopeFilter.FILE_SUBTYPE_NONE
import nz.mega.sdk.MegaNodeScopeFilter.FILE_SUBTYPE_RAW
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubCategoryIntMapperTest {

    private val underTest = SubCategoryIntMapper()

    @ParameterizedTest(name = "when sub-category is {0}, then the int value is {1}")
    @MethodSource("provideParameters")
    fun `test that the sub-category maps to the correct int value`(
        subCategory: SubCategory,
        expected: Int,
    ) {
        val actual = underTest(subCategory)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "when int value is {1}, then the sub-category is {0}")
    @MethodSource("provideParameters")
    fun `test that the int value maps back to the correct sub-category`(
        expected: SubCategory,
        value: Int,
    ) {
        val actual = underTest(value)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that an unknown int value throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> { underTest(UNKNOWN_VALUE) }
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(SubCategory.All, FILE_SUBTYPE_NONE),
        Arguments.of(SubCategory.Gif, FILE_SUBTYPE_GIF),
        Arguments.of(SubCategory.Raw, FILE_SUBTYPE_RAW),
    )

    companion object {
        private const val UNKNOWN_VALUE = -1
    }
}
