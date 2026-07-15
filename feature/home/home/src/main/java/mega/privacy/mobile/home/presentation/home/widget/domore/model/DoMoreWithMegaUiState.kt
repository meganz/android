package mega.privacy.mobile.home.presentation.home.widget.domore.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.mobile.home.presentation.home.widget.domore.DoMoreWithMegaItem

/**
 * UI state for the "Do more with MEGA" home widget.
 *
 * [mega.privacy.android.domain.featuretoggle.ApiFeatures.DoMoreWithMEGA] feature flag).
 * @property items the action items to display, already sorted by their order.
 * @property createAlbumErrorMessage validation error to surface in the dialog, if any.
 * @property albumCreatedEvent emitted once an album is created, carrying the created album so the
 * UI can show a confirmation snackbar and offer to open it.
 * @property isCameraUploadsEnabled whether Camera uploads is currently enabled, used to decide
 * whether the Camera uploads shortcut opens its settings or the permissions screen.
 * @property hasPreviouslyEnabledCameraUploads whether the user has enabled Camera uploads before
 * (the enabled preference has been set at least once). When true the Camera uploads shortcut skips
 * the permissions onboarding even if Camera uploads is currently disabled.
 */
data class DoMoreWithMegaUiState(
    val items: List<DoMoreWithMegaItem> = emptyList(),
    val createAlbumErrorMessage: StateEventWithContent<String> = consumed(),
    val albumCreatedEvent: StateEventWithContent<CreatedAlbum> = consumed(),
    val isCameraUploadsEnabled: Boolean = false,
    val hasPreviouslyEnabledCameraUploads: Boolean = false,
)

/**
 * A user album that was just created from the "Do more with MEGA" widget.
 *
 * @property id the created album's id.
 * @property name the name the album was created with.
 */
data class CreatedAlbum(
    val id: AlbumId,
    val name: String,
)
