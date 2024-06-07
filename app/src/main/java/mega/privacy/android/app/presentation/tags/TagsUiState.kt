package mega.privacy.android.app.presentation.tags

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.meeting.chat.model.InfoToShow

/**
 * Tags UI state.
 *
 * @property tags List of tags.
 * @property isError If there is an error.
 * @property message Message to show.
 * @property informationMessage Information message.
 */
data class TagsUiState(
    val tags: List<String> = emptyList(),
    val isError: Boolean = false,
    val message: String? = null,
    val informationMessage: StateEventWithContent<InfoToShow> = consumed(),
)
