package mega.privacy.android.app.presentation.search.model

import androidx.annotation.StringRes

/**
 * Entity class to represent the filter option
 *
 * @property id The id of the filter option
 * @property title The title of the filter option
 * @property isSelected The flag to indicate if the filter option is selected
 */
data class FilterOptionEntity(
    val id: Int,
    @StringRes val title: Int,
    val isSelected: Boolean,
)
