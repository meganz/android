package mega.privacy.android.feature.photos.presentation.timeline.revamp

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.feature.photos.model.PhotosNodeContentItemV2

/**
 * UI state for the Timeline Revamp screen
 */
@Stable
sealed interface TimelineRevampUiState {

    /**
     * Loading state, shown while the timeline sections are being fetched
     */
    data object Loading : TimelineRevampUiState

    /**
     * Empty state, shown once the timeline sections have been fetched and there is no media to display
     */
    data object Empty : TimelineRevampUiState

    /**
     * Loaded state holding the timeline sections to display
     *
     * @property sections the list of media timeline sections
     * @property sectionStartOffsets the global slot index where each section begins (parallel to
     * [sections]): `sectionStartOffsets[i]` is the sum of the counts of all sections before `i`.
     * @property loadedNodes the media nodes that have been loaded so far, keyed by their global
     * index across all sections (section 0 occupies indices `0 until count0`, section 1 the next
     * `count1`, and so on). Slots not present in the map are not yet loaded and render as shimmer.
     */
    data class Data(
        val sections: List<MediaTimelineSection>,
        val sectionStartOffsets: List<Int>,
        val loadedNodes: Map<Int, PhotosNodeContentItemV2>,
    ) : TimelineRevampUiState
}
