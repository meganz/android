package mega.privacy.android.feature.photos.presentation.timeline.revamp

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.feature.photos.model.PhotosNodeContentItemV2
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabSortOptions
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard

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
     * @property isHiddenNodesEnabled whether the hidden-nodes feature is enabled for the account;
     * drives the sensitive-item blur in the grid.
     * @property gridSize the selected grid size, driving the number of columns in the grid.
     * @property currentSort the selected sort option (Newest / Oldest), shown as the checked option in
     * the sort dialog.
     * @property selectedPeriod the selected time period, deciding whether the grid or a Year / Month
     * card list is shown.
     * @property periodCards the Year or Month summary cards shown when [selectedPeriod] is
     * [MediaTimePeriod.Years] or [MediaTimePeriod.Months]; empty otherwise.
     * @property arePeriodCardsLoading whether the Year / Month summary cards are still being built
     * for the current [selectedPeriod]; drives the card shimmer while [periodCards] is empty.
     * @property takenDownDialogEvent triggered when a taken-down node is tapped, so the screen can
     * show the dispute dialog.
     */
    data class Data(
        val sections: List<MediaTimelineSection>,
        val sectionStartOffsets: List<Int>,
        val loadedNodes: Map<Int, PhotosNodeContentItemV2>,
        val isHiddenNodesEnabled: Boolean = false,
        val gridSize: TimelineGridSize = TimelineGridSize.Default,
        val currentSort: TimelineTabSortOptions = TimelineTabSortOptions.Newest,
        val selectedPeriod: MediaTimePeriod = MediaTimePeriod.All,
        val periodCards: List<PhotosNodeListCard> = emptyList(),
        val arePeriodCardsLoading: Boolean = false,
        val takenDownDialogEvent: StateEvent = consumed,
    ) : TimelineRevampUiState
}
