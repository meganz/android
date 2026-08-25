package mega.privacy.android.feature.photos.presentation.timeline.revamp

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.photos.Sort
import mega.privacy.android.feature.photos.presentation.timeline.revamp.TimelineRevampSortConfiguration.Companion.toLegacySort
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TimelineRevampSortConfigurationTest {

    @ParameterizedTest(name = "{0} {1} maps to {2}")
    @MethodSource("sortOrders")
    fun `test that sortOrder maps the option and direction to the matching SDK order`(
        option: TimelineRevampSortOption,
        direction: SortDirection,
        expected: SortOrder,
    ) {
        val underTest = TimelineRevampSortConfiguration(option, direction)

        assertThat(underTest.sortOrder).isEqualTo(expected)
    }

    @Test
    fun `test that the default is Date added newest first`() {
        assertThat(TimelineRevampSortConfiguration.Default.option)
            .isEqualTo(TimelineRevampSortOption.DateAdded)
        assertThat(TimelineRevampSortConfiguration.Default.direction)
            .isEqualTo(SortDirection.Descending)
        assertThat(TimelineRevampSortConfiguration.Default.sortOrder)
            .isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
    }

    @ParameterizedTest(name = "{0} {1} maps to {2}")
    @MethodSource("legacySorts")
    fun `test that toLegacySort maps the direction and ignores the option`(
        option: TimelineRevampSortOption,
        direction: SortDirection,
        expected: Sort,
    ) {
        val underTest = TimelineRevampSortConfiguration(option, direction)

        assertThat(underTest.toLegacySort()).isEqualTo(expected)
    }

    @Test
    fun `test that every option defaults to newest first`() {
        TimelineRevampSortOption.entries.forEach { option ->
            assertThat(option.defaultSortDirection).isEqualTo(SortDirection.Descending)
        }
    }

    private fun sortOrders(): Stream<Arguments> = Stream.of(
        Arguments.of(
            TimelineRevampSortOption.DateTaken,
            SortDirection.Descending,
            SortOrder.ORDER_MEDIATS_DESC,
        ),
        Arguments.of(
            TimelineRevampSortOption.DateTaken,
            SortDirection.Ascending,
            SortOrder.ORDER_MEDIATS_ASC,
        ),
        Arguments.of(
            TimelineRevampSortOption.DateAdded,
            SortDirection.Descending,
            SortOrder.ORDER_MODIFICATION_DESC,
        ),
        Arguments.of(
            TimelineRevampSortOption.DateAdded,
            SortDirection.Ascending,
            SortOrder.ORDER_MODIFICATION_ASC,
        ),
    )

    private fun legacySorts(): Stream<Arguments> = Stream.of(
        Arguments.of(TimelineRevampSortOption.DateTaken, SortDirection.Descending, Sort.NEWEST),
        Arguments.of(TimelineRevampSortOption.DateTaken, SortDirection.Ascending, Sort.OLDEST),
        Arguments.of(TimelineRevampSortOption.DateAdded, SortDirection.Descending, Sort.NEWEST),
        Arguments.of(TimelineRevampSortOption.DateAdded, SortDirection.Ascending, Sort.OLDEST),
    )
}
