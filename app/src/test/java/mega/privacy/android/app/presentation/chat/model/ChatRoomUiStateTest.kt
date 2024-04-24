package mega.privacy.android.app.presentation.chat.model

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatRoomUiStateTest {

    @Test
    fun `test that true is returned when the chat room changes contain the specific change`() {
        val chatroomUiState = ChatRoomUiState(
            changes = listOf(ChatRoomChange.ChatMode)
        )

        val actual = chatroomUiState.hasChanged(ChatRoomChange.ChatMode)

        assertThat(actual).isTrue()
    }
}
