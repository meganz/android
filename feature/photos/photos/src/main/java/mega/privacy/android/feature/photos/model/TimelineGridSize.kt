package mega.privacy.android.feature.photos.model

import androidx.annotation.StringRes
import mega.privacy.android.shared.resources.R as sharedR

/**
 * A model representing the timeline grid size settings. See the usage in [mega.privacy.android.feature.photos.presentation.timeline.TimelineTabScreen].
 *
 * @property nameResId The string resource ID of the name of the grid size.
 * @property portrait The number of photos to show each row in portrait mode.
 * @property landscape The number of photos to show each row  in landscape mode.
 */
enum class TimelineGridSize(
    @StringRes val nameResId: Int,
    val portrait: Int,
    val landscape: Int,
) {
    Large(nameResId = sharedR.string.timeline_tab_grid_size_large, portrait = 1, landscape = 2),
    Default(nameResId = sharedR.string.timeline_tab_grid_size_default, portrait = 3, landscape = 5),
    Compact(nameResId = sharedR.string.timeline_tab_grid_size_compact, portrait = 5, landscape = 9),
}
