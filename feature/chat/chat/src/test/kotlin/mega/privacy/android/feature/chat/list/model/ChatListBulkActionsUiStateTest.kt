package mega.privacy.android.feature.chat.list.model

import androidx.compose.runtime.saveable.SaverScope
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ChatListBulkActionsUiStateTest {

    private val saver = ChatListBulkActionsUiState.Saver

    @Test
    fun `test that the saver round trips the Hidden state`() {
        val restored = roundTrip(ChatListBulkActionsUiState.Hidden)

        assertThat(restored).isEqualTo(ChatListBulkActionsUiState.Hidden)
    }

    @Test
    fun `test that the saver round trips the ShowingActions state`() {
        val original = ChatListBulkActionsUiState.ShowingActions(chatIds = setOf(1L, 2L, 3L))

        val restored = roundTrip(original)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `test that the saver round trips the Confirming state`() {
        val original = ChatListBulkActionsUiState.Confirming(
            chatIds = setOf(4L, 5L),
            actionId = "action",
            confirmation = ChatRoomMenuItemConfirmation(
                title = 1,
                message = 2,
                confirmButtonText = 3,
                testTag = "confirm",
            ),
        )

        val restored = roundTrip(original)

        assertThat(restored).isEqualTo(original)
    }

    @Suppress("UNCHECKED_CAST")
    private fun roundTrip(state: ChatListBulkActionsUiState): ChatListBulkActionsUiState? {
        val saver = saver as androidx.compose.runtime.saveable.Saver<ChatListBulkActionsUiState, Any>
        val saved = with(saver) { SaverScope { true }.save(state) }
        return saved?.let { saver.restore(it) }
    }
}
