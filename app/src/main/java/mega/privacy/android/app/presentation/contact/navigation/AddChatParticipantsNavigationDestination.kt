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
import mega.privacy.android.app.R
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.feature.contact.navigation.AddChatParticipantsEntry
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.AddChatParticipantsNavKey

/**
 * Registers the [AddChatParticipantsNavKey] destination. Behind [AppFeatures.ContactsComposeUI] either
 * renders the Compose [AddChatParticipantsEntry] picker (flag on) or launches the legacy
 * [AddContactActivity] in add-chat-participants mode (flag off). Both paths publish the selected
 * contact emails as a `List<String>` under [AddChatParticipantsNavKey.KEY].
 *
 * TODO: Move this entry to the feature module once the feature flag is removed
 */
fun EntryProviderScope<NavKey>.addChatParticipantsDestination(navigationHandler: NavigationHandler) {
    entry<AddChatParticipantsNavKey> { key ->
        FeatureFlagGate(
            feature = AppFeatures.ContactsComposeUI,
            disabled = {
                LegacyAddChatParticipantsEntry(
                    chatId = key.chatId,
                    onResult = { emails ->
                        navigationHandler.returnResult(AddChatParticipantsNavKey.KEY, emails)
                    },
                )
            },
            enabled = {
                AddChatParticipantsEntry(
                    navigationHandler = navigationHandler,
                    chatId = key.chatId,
                )
            },
        )
    }
}

/**
 * Launches the legacy [AddContactActivity] in add-chat-participants mode for [chatId] and forwards the
 * selected emails (empty when cancelled) through [onResult], which publishes them and pops the entry.
 */
@Composable
private fun LegacyAddChatParticipantsEntry(chatId: Long, onResult: (List<String>) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = AddChatParticipantsContract(),
    ) { emails ->
        onResult(emails.orEmpty())
    }
    LaunchedEffect(Unit) {
        launcher.launch(chatId)
    }
}

private class AddChatParticipantsContract : ActivityResultContract<Long, List<String>?>() {

    override fun createIntent(context: Context, input: Long): Intent =
        Intent(context, AddContactActivity::class.java)
            .putExtra(Constants.INTENT_EXTRA_KEY_CONTACT_TYPE, Constants.CONTACT_TYPE_MEGA)
            .putExtra(Constants.INTENT_EXTRA_KEY_CHAT, true)
            .putExtra(Constants.INTENT_EXTRA_KEY_CHAT_ID, input)
            .putExtra(
                Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
                context.getString(R.string.add_participants_menu_item),
            )

    override fun parseResult(resultCode: Int, intent: Intent?): List<String>? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        return intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)?.toList()
    }
}
