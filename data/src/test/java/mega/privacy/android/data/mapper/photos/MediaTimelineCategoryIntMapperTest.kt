package mega.privacy.android.data.mapper.photos

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Category
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaTimelineCategoryIntMapperTest {

    private val underTest = MediaTimelineCategoryIntMapper()

    @ParameterizedTest(name = "when category is {0}, then the int value is {1}")
    @MethodSource("provideParameters")
    fun `test that the category maps to the correct int value`(
        category: Category,
        expected: Int,
    ) {
        val actual = underTest(category)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "when int value is {1}, then the category is {0}")
    @MethodSource("provideParameters")
    fun `test that the int value maps back to the correct category`(
        expected: Category,
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
        Arguments.of(Category.Photos, MegaApiJava.FILE_TYPE_PHOTO),
        Arguments.of(Category.Videos, MegaApiJava.FILE_TYPE_VIDEO),
        Arguments.of(Category.All, MegaApiJava.FILE_TYPE_ALL_VISUAL_MEDIA),
    )

    companion object {
        private const val UNKNOWN_VALUE = -1
    }
}
