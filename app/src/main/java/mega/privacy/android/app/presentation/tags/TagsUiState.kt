package mega.privacy.android.app.presentation.tags

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import mega.privacy.android.app.presentation.meeting.chat.model.InfoToShow

/**
 * Tags UI state.
 *
 * @property nodeTags List of tags added to that particular node.
 * @property tags searched list of tags for the user, will contain node tags too.
 * @property isError If there is an error.
 * @property message Message to show.
 * @property showMaxTagsError If the maximum number of tags has been reached.
 * @property tagsUpdatedEvent If the tags have been updated.
 * @property searchText Search text.
 * @property informationMessage Information message.
 */
data class TagsUiState(
    val nodeTags: ImmutableList<String> = persistentListOf(),
    val tags: ImmutableList<String> = persistentListOf(),
    val isError: Boolean = false,
    val message: String? = null,
    val showMaxTagsError: StateEvent = consumed,
    val tagsUpdatedEvent: StateEventWithContent<TagUpdate> = consumed(),
    val searchText: String = "",
    val informationMessage: StateEventWithContent<InfoToShow> = consumed(),
)
