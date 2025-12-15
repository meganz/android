package mega.privacy.android.app.presentation.chat.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.meeting.chat.ChatActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ChatListNavKey

fun EntryProviderScope<NavKey>.chatListLegacyDestination(removeDestination: () -> Unit) {
    entry<ChatListNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, ChatActivity::class.java).apply {
                putExtra(ChatActivity.OPEN_CHAT_LIST, true)
                putExtra(ChatActivity.CREATE_NEW_CHAT, key.createNewChat)
            }
            context.startActivity(intent)
            removeDestination()
        }
    }
}
