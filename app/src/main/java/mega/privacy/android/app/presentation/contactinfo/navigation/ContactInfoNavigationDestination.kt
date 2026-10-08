package mega.privacy.android.app.presentation.contactinfo.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity
import mega.privacy.android.app.presentation.contact.navigation.ContactAttachmentHost
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.feature.contact.info.navigation.ContactInfoEntry
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.navigation.destination.NodeAttachmentHistoryNavKey

/**
 * Registers the [ContactInfoNavKey] destination. Behind [ApiFeatures.ContactComposeFeature] either
 * renders the Compose [ContactInfoEntry] contact info screen (flag on) or launches the legacy
 * [ContactInfoActivity] and pops itself (flag off).
 */
fun EntryProviderScope<NavKey>.contactInfoDestination(navigationHandler: NavigationHandler) {
    entry<ContactInfoNavKey> { navKey ->
        FeatureFlagGate(
            feature = ApiFeatures.ContactComposeFeature,
            disabled = {
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    val intent = Intent(context, ContactInfoActivity::class.java)
                    navKey.email?.let { intent.putExtra(Constants.NAME, it) }
                    navKey.chatId?.let { intent.putExtra(Constants.HANDLE, it) }
                    context.startActivity(intent)
                    navigationHandler.back()
                }
            },
            enabled = {
                ComposeContactInfo(
                    navigationHandler = navigationHandler,
                    navKey = navKey,
                )
            },
        )
    }
}

/**
 * Hosts [ContactInfoEntry] together with the app-module collaborators the send file and share
 * contact actions need.
 */
@Composable
private fun ComposeContactInfo(
    navigationHandler: NavigationHandler,
    navKey: ContactInfoNavKey,
) {
    ContactAttachmentHost(navigationHandler) { onSendFileToChat, onShareContact ->
        ContactInfoEntry(
            navigationHandler = navigationHandler,
            email = navKey.email,
            chatId = navKey.chatId,
            onSendFileToChat = onSendFileToChat,
            onShareContact = onShareContact,
        )
    }
}

/**
 * Registers the [NodeAttachmentHistoryNavKey] destination, bridging to the legacy
 * [NodeAttachmentHistoryActivity] that lists the files shared in a chat.
 */
fun EntryProviderScope<NavKey>.nodeAttachmentHistoryDestination(
    navigationHandler: NavigationHandler,
) {
    entry<NodeAttachmentHistoryNavKey>(metadata = transparentMetadata()) { navKey ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.startActivity(
                Intent(context, NodeAttachmentHistoryActivity::class.java)
                    .putExtra("chatId", navKey.chatId)
            )
            navigationHandler.back()
        }
    }
}
