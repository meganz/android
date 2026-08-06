package mega.privacy.android.data.mapper.photos

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Favourites
import nz.mega.sdk.MegaSearchFilter.BOOL_FILTER_DISABLED
import nz.mega.sdk.MegaSearchFilter.BOOL_FILTER_ONLY_FALSE
import nz.mega.sdk.MegaSearchFilter.BOOL_FILTER_ONLY_TRUE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaTimelineFavouritesIntMapperTest {

    private val underTest = MediaTimelineFavouritesIntMapper()

    @ParameterizedTest(name = "when favourites is {0}, then the int value is {1}")
    @MethodSource("provideParameters")
    fun `test that the favourites maps to the correct int value`(
        favourites: Favourites,
        expected: Int,
    ) {
        val actual = underTest(favourites)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "when int value is {1}, then the favourites is {0}")
    @MethodSource("provideParameters")
    fun `test that the int value maps back to the correct favourites`(
        expected: Favourites,
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
        Arguments.of(Favourites.All, BOOL_FILTER_DISABLED),
        Arguments.of(Favourites.Favourites, BOOL_FILTER_ONLY_TRUE),
        Arguments.of(Favourites.NonFavourites, BOOL_FILTER_ONLY_FALSE),
    )

    companion object {
        private const val UNKNOWN_VALUE = -1
    }
}
