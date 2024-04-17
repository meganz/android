package mega.privacy.android.app.presentation.search.model

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.search.DateFilterOption

/**
 * Data class for date filter and string res
 *
 * @param date date filter
 * @param title string res for date filter
 */
data class DateFilterWithName(val date: DateFilterOption, @StringRes val title: Int)
