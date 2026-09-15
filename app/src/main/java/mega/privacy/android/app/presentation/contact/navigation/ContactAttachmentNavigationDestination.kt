package mega.privacy.android.app.presentation.contact.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.main.megachat.ContactAttachmentActivity
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.feature.contact.attachment.navigation.ContactAttachmentEntry
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.ChatNavKey.Companion.LEGACY_CHAT_ID
import mega.privacy.android.navigation.destination.ChatNavKey.Companion.LEGACY_MESSAGE_ID
import mega.privacy.android.navigation.destination.ContactAttachmentNavKey

/**
 * Registers the [ContactAttachmentNavKey] destination. Behind [ApiFeatures.ContactComposeFeature]
 * either renders the Compose [ContactAttachmentEntry] screen (flag on) or launches the legacy
 * [ContactAttachmentActivity] and pops itself (flag off).
 */
fun EntryProviderScope<NavKey>.contactAttachmentDestination(navigationHandler: NavigationHandler) {
    entry<ContactAttachmentNavKey> { navKey ->
        FeatureFlagGate(
            feature = ApiFeatures.ContactComposeFeature,
            disabled = {
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    context.startActivity(
                        Intent(context, ContactAttachmentActivity::class.java).apply {
                            putExtra(LEGACY_CHAT_ID, navKey.chatId)
                            putExtra(LEGACY_MESSAGE_ID, navKey.messageId)
                        }
                    )
                    navigationHandler.back()
                }
            },
            enabled = {
                ContactAttachmentEntry(
                    navigationHandler = navigationHandler,
                    chatId = navKey.chatId,
                    messageId = navKey.messageId,
                )
            },
        )
    }
}
