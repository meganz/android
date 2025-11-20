package mega.privacy.android.app.presentation.chat.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.NEW_MESSAGE_CHAT_LINK
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.ChatsNavKey
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Deep link handler for chats deep links
 */
class ChatsDeepLinkHandler @Inject constructor(
    private val snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? = when (regexPatternType) {
        NEW_MESSAGE_CHAT_LINK -> if (isLoggedIn) {
            listOf(ChatsNavKey)
        } else {
            snackbarEventQueue.queueMessage(sharedR.string.general_alert_not_logged_in)
            emptyList()
        }

        else -> null
    }
}