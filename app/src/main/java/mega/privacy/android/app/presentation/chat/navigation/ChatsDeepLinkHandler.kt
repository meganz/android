package mega.privacy.android.app.presentation.chat.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.NEW_MESSAGE_CHAT_LINK
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.destination.ChatsNavKey
import javax.inject.Inject

/**
 * Deep link handler for chats deep links
 */
class ChatsDeepLinkHandler @Inject constructor() : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? =
        if (regexPatternType == NEW_MESSAGE_CHAT_LINK) {
            listOf(ChatsNavKey)
        } else {
            null
        }
}