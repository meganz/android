package mega.privacy.android.app.presentation.videosection.model

/**
 * Entity class to represent the videos filter option.
 *
 * @property id The id of the filter option.
 * @property title The title of the filter option.
 * @property isSelected The flag to indicate if the filter option is selected.
 */
data class VideosFilterOptionEntity(
    val id: Int,
    val title: String,
    val isSelected: Boolean,
)
