package mega.privacy.android.data.mapper.photos

import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Sensitivity
import nz.mega.sdk.MegaNodeScopeFilter
import javax.inject.Inject

/**
 * Two-way mapper between [Sensitivity] and the matching
 * [MegaNodeScopeFilter] sensitivity Int value.
 */
internal class MediaTimelineSensitivityIntMapper @Inject constructor() {

    /**
     * Maps a [Sensitivity] into the SDK sensitivity Int value.
     */
    operator fun invoke(sensitivity: Sensitivity): Int = when (sensitivity) {
        Sensitivity.ShowAll -> MegaNodeScopeFilter.SENSITIVITY_SHOW_ALL
        Sensitivity.HideSensitive -> MegaNodeScopeFilter.SENSITIVITY_HIDE_SENSITIVE
    }

    /**
     * Maps an SDK sensitivity Int value back into a [Sensitivity].
     */
    operator fun invoke(value: Int): Sensitivity = when (value) {
        MegaNodeScopeFilter.SENSITIVITY_SHOW_ALL -> Sensitivity.ShowAll
        MegaNodeScopeFilter.SENSITIVITY_HIDE_SENSITIVE -> Sensitivity.HideSensitive
        else -> throw IllegalArgumentException("Unknown sensitivity value: $value")
    }
}
