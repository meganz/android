package mega.privacy.android.app.presentation.contact.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.startconversation.StartConversationActivity
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.feature.contact.group.create.navigation.NewChatEntry
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.NewChatNavKey
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE

/**
 * Registers the [NewChatNavKey] destination. Behind [ApiFeatures.ContactComposeFeature] either renders the
 * Compose [NewChatEntry] screen (flag on) or launches the legacy [StartConversationActivity] (flag off).
 *
 * The Compose screen publishes the chosen [NewChatNavKey.NewChatResult] under [NewChatNavKey.KEY] and the
 * consuming caller creates the chat. The legacy screen creates the chat itself, so this destination
 * replaces itself with the created chat instead of publishing a result.
 */
fun EntryProviderScope<NavKey>.newChatLegacyDestination(navigationHandler: NavigationHandler) {
    entry<NewChatNavKey> {
        FeatureFlagGate(
            feature = ApiFeatures.ContactComposeFeature,
            disabled = {
                LegacyStartConversationEntry(
                    onChatCreated = { chatId ->
                        navigationHandler.navigate(ShowChatMessagesNavKey(chatId))
                        navigationHandler.remove(NewChatNavKey)
                    },
                    onCancelled = { navigationHandler.remove(NewChatNavKey) },
                )
            },
            enabled = {
                NewChatEntry(navigationHandler = navigationHandler)
            },
        )
    }
}

/**
 * Launches the legacy [StartConversationActivity] and reports the id of the chat it created through
 * [onChatCreated], or [onCancelled] when no chat was created.
 */
@Composable
private fun LegacyStartConversationEntry(
    onChatCreated: (Long) -> Unit,
    onCancelled: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = StartConversationContract(),
    ) { chatId ->
        if (chatId != null) onChatCreated(chatId) else onCancelled()
    }
    LaunchedEffect(Unit) {
        launcher.launch(Unit)
    }
}

private class StartConversationContract : ActivityResultContract<Unit, Long?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        StartConversationActivity.getChatIntent(context)

    override fun parseResult(resultCode: Int, intent: Intent?): Long? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        return intent.getLongExtra(StartConversationActivity.EXTRA_NEW_CHAT_ID, MEGACHAT_INVALID_HANDLE)
            .takeIf { it != MEGACHAT_INVALID_HANDLE }
    }
}
