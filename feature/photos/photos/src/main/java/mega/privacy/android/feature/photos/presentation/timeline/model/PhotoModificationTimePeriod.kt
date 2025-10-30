package mega.privacy.android.feature.photos.presentation.timeline.model

import androidx.annotation.StringRes
import mega.privacy.android.shared.resources.R as SharedR

enum class PhotoModificationTimePeriod(@StringRes val stringResId: Int) {
    Years(stringResId = SharedR.string.media_timeline_filter_years_chip_text),
    Months(stringResId = SharedR.string.media_timeline_filter_months_chip_text),
    Days(stringResId = SharedR.string.media_timeline_filter_days_chip_text),
    All(stringResId = SharedR.string.media_timeline_default_chip_text)
}
