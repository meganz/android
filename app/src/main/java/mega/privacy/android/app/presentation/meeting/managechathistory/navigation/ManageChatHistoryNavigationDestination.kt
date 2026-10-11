package mega.privacy.android.app.presentation.meeting.managechathistory.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.meeting.managechathistory.view.screen.ManageChatHistoryActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.feature.chat.managehistory.navigation.ManageChatHistoryEntry
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.navigation.destination.ManageChatHistoryNavKey
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE

/**
 * Registers the [ManageChatHistoryNavKey] destination. Behind
 * [ApiFeatures.CustomisableBottomNavigation] either renders the Compose [ManageChatHistoryEntry]
 * screen (flag on) or launches the legacy [ManageChatHistoryActivity] and pops itself (flag off).
 */
fun EntryProviderScope<NavKey>.manageChatHistoryDestination(
    navigationHandler: NavigationHandler,
) {
    entry<ManageChatHistoryNavKey> { key ->
        FeatureFlagGate(
            feature = ApiFeatures.CustomisableBottomNavigation,
            disabled = {
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    context.startActivity(
                        Intent(context, ManageChatHistoryActivity::class.java).apply {
                            putExtra(ChatNavKey.LEGACY_CHAT_ID, key.chatId)
                            key.email?.let { putExtra(Constants.EMAIL, it) }
                        }
                    )
                    navigationHandler.back()
                }
            },
            enabled = {
                ManageChatHistoryEntry(
                    navigationHandler = navigationHandler,
                    chatId = key.chatId.takeIf { it != MEGACHAT_INVALID_HANDLE },
                    email = key.email,
                )
            },
        )
    }
}
