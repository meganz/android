package mega.privacy.android.feature.chat.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Entry point for the single-activity chat room, hosted in MegaActivity behind the `nchat` remote
 * flag (`ApiFeatures.SingleActivityChatRoom`).
 *
 * Hosted by the app module's gated `ChatNavKey` and `ShowChatMessagesNavKey` destinations.
 *
 * @param chatId Id of the chat room to open.
 * @param action Legacy intent action the chat room was opened with, if any.
 * @param link Chat link the chat room was opened from, if any.
 * @param openFromList Whether the chat room was opened from the chat list.
 * @param onBack Leaves the chat room.
 */
@Composable
fun ChatRoomEntry(
    chatId: Long,
    action: String?,
    link: String?,
    openFromList: Boolean,
    onBack: () -> Unit,
) {
    MegaScaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag(CHAT_ROOM_ENTRY_TAG),
        topBar = {
            MegaTopAppBar(
                title = "",
                navigationType = AppBarNavigationType.Back(onBack),
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

/**
 * Test tag of the [ChatRoomEntry] screen.
 */
const val CHAT_ROOM_ENTRY_TAG = "chat_room_entry"

@CombinedThemePreviews
@Composable
private fun ChatRoomEntryPreview() {
    AndroidThemeForPreviews {
        ChatRoomEntry(
            chatId = 1L,
            action = null,
            link = null,
            openFromList = false,
            onBack = {},
        )
    }
}
