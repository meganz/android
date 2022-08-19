package mega.privacy.android.app.meeting.chats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/**
 * Chat tabs view model
 *
 * @property state      Needed to save current view state
 */
class ChatTabsViewModel constructor(
    private val state: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val STATE_PAGER_POSITION = "STATE_PAGER_POSITION"
    }

    fun getCurrentPosition(): Int =
        state[STATE_PAGER_POSITION] ?: 0

    fun setCurrentPosition(position: Int) {
        state[STATE_PAGER_POSITION] = position
    }
}
