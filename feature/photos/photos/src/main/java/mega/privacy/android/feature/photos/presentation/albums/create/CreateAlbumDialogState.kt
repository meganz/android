package mega.privacy.android.feature.photos.presentation.albums.create

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.navigation.destination.CreateAlbumDialogResult

/**
 * UI state for [CreateAlbumDialogM3].
 *
 * @property placeholder the suggested album name shown as the input placeholder, recomputed as the
 * user's albums change so it stays free of collisions.
 * @property errorMessage validation error to display, if any.
 * @property albumCreatedEvent emitted once an album has been created successfully.
 */
data class CreateAlbumDialogState(
    val placeholder: String = "",
    val errorMessage: StateEventWithContent<String> = consumed(),
    val albumCreatedEvent: StateEventWithContent<CreateAlbumDialogResult> = consumed(),
)
