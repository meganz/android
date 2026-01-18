package mega.privacy.android.app.presentation.meeting.managechathistory.navigation

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.meeting.managechathistory.view.screen.ManageChatHistoryActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ManageChatHistoryNavKey

fun EntryProviderScope<NavKey>.manageChatHistoryLegacyDestination(removeDestination: (NavKey) -> Unit) {
    entry<ManageChatHistoryNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.startActivity(createManageChatHistoryIntent(context, key))
            removeDestination(key)
        }
    }
}

/**
 * Creates an Intent for ManageChatHistoryActivity with all relevant extras
 *
 * @param context Context to create the Intent
 * @param key ManageChatHistoryNavKey containing all navigation parameters
 * @return Intent configured for ManageChatHistoryActivity
 */
private fun createManageChatHistoryIntent(context: Context, key: ManageChatHistoryNavKey): Intent {
    return Intent(context, ManageChatHistoryActivity::class.java).apply {
        putExtra(Constants.CHAT_ID, key.chatId)
        key.email?.let { putExtra(Constants.EMAIL, it) }
    }
}
