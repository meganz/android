package mega.privacy.android.app.presentation.search.mapper

import androidx.annotation.StringRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.search.DateFilterOption
import javax.inject.Inject

/**
 * Mapper used to map the date filter options to string resources
 */
class DateFilterOptionStringResMapper @Inject constructor() {

    /**
     * invoke
     *
     * @param dateFilterOption date filter option
     */
    @StringRes
    operator fun invoke(dateFilterOption: DateFilterOption): Int = when (dateFilterOption) {
        DateFilterOption.Today -> R.string.search_dropdown_chip_filter_type_date_today
        DateFilterOption.Last7Days -> R.string.search_dropdown_chip_filter_type_date_last_seven_days
        DateFilterOption.Last30Days -> R.string.search_dropdown_chip_filter_type_date_last_thirty_days
        DateFilterOption.ThisYear -> R.string.search_dropdown_chip_filter_type_date_this_year
        DateFilterOption.LastYear -> R.string.search_dropdown_chip_filter_type_date_last_year
        DateFilterOption.Older -> R.string.search_dropdown_chip_filter_type_date_older
    }
}
