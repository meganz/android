package mega.privacy.android.app.presentation.search.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.search.DateFilterOption
import javax.inject.Inject

/**
 * Mapper used to map the date filter options to string
 */
class DateFilterOptionStringMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * invoke
     *
     * @param dateFilterOption date filter option
     */
    operator fun invoke(dateFilterOption: DateFilterOption): String = when (dateFilterOption) {
        DateFilterOption.Today -> context.getString(R.string.search_dropdown_chip_filter_type_date_today)
        DateFilterOption.Last7Days -> context.getString(R.string.search_dropdown_chip_filter_type_date_last_seven_days)
        DateFilterOption.Last30Days -> context.getString(R.string.search_dropdown_chip_filter_type_date_last_thirty_days)
        DateFilterOption.ThisYear -> context.getString(R.string.search_dropdown_chip_filter_type_date_this_year)
        DateFilterOption.LastYear -> context.getString(R.string.search_dropdown_chip_filter_type_date_last_year)
        DateFilterOption.Older -> context.getString(R.string.search_dropdown_chip_filter_type_date_older)
    }
}