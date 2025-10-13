package mega.privacy.android.app.presentation.chat.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator

fun EntryProviderBuilder<NavKey>.chatLegacyDestination(removeDestination: () -> Unit) {
    entry<ChatNavKey> { key ->
        val context = LocalContext.current
        val megaNavigator = rememberMegaNavigator()
        LaunchedEffect(Unit) {
            megaNavigator.openChat(context, key.chatId, key.action)
            removeDestination()
        }
    }
}
