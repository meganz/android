package mega.privacy.android.app.presentation.search.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.search.DateFilterOption
import org.junit.Test

class DateFilterOptionStringResMapperTest {
    private val dateFilterOptionStringResMapper = DateFilterOptionStringResMapper()

    @Test
    fun `test that map date filter option to string resource`() {
        assertThat(dateFilterOptionStringResMapper(DateFilterOption.Today))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_date_today)
        assertThat(dateFilterOptionStringResMapper(DateFilterOption.Last7Days))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_date_last_seven_days)
        assertThat(dateFilterOptionStringResMapper(DateFilterOption.Last30Days))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_date_last_thirty_days)
        assertThat(dateFilterOptionStringResMapper(DateFilterOption.ThisYear))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_date_this_year)
        assertThat(dateFilterOptionStringResMapper(DateFilterOption.LastYear))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_date_last_year)
        assertThat(dateFilterOptionStringResMapper(DateFilterOption.Older))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_date_older)
    }
}
