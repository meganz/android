package mega.privacy.android.app.presentation.chat.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import mega.privacy.android.navigation.destination.Chat
import mega.privacy.android.navigation.extensions.rememberMegaNavigator

fun NavGraphBuilder.chatLegacyDestination(removeDestination: () -> Unit) {
    composable<Chat> {
        val context = LocalContext.current
        val megaNavigator = rememberMegaNavigator()
        val chat = it.toRoute<Chat>()
        LaunchedEffect(Unit) {
            megaNavigator.openChat(context, chat.chatId, chat.action)
            removeDestination()
        }
    }
}
