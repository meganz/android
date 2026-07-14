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
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.feature.contact.group.create.navigation.NewChatEntry
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.NewChatNavKey

/**
 * Registers the [NewChatNavKey] destination. Behind [AppFeatures.ContactsComposeUI] either renders the
 * Compose [NewChatEntry] screen (flag on) or launches the legacy [AddContactActivity] new-chat picker
 * (flag off). Both paths publish the chosen [NewChatNavKey.NewChatResult] under [NewChatNavKey.KEY]; the
 * consuming caller creates the chat and sends its content into it.
 */
fun EntryProviderScope<NavKey>.newChatLegacyDestination(navigationHandler: NavigationHandler) {
    entry<NewChatNavKey> {
        FeatureFlagGate(
            feature = AppFeatures.ContactsComposeUI,
            disabled = {
                LegacyNewChatEntry(
                    onResult = { result ->
                        navigationHandler.returnResult(NewChatNavKey.KEY, result)
                    },
                )
            },
            enabled = {
                NewChatEntry(navigationHandler = navigationHandler)
            },
        )
    }
}

/**
 * Launches the legacy [AddContactActivity] new-chat picker and forwards the parsed
 * [NewChatNavKey.NewChatResult] (null when cancelled) through [onResult].
 */
@Composable
private fun LegacyNewChatEntry(
    onResult: (NewChatNavKey.NewChatResult?) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = NewChatContract(),
    ) { result ->
        onResult(result)
    }
    LaunchedEffect(Unit) {
        launcher.launch(Unit)
    }
}

private class NewChatContract : ActivityResultContract<Unit, NewChatNavKey.NewChatResult?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(context, AddContactActivity::class.java)
            .putExtra(AddContactActivity.EXTRA_CONTACT_TYPE, Constants.CONTACT_TYPE_MEGA)

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): NewChatNavKey.NewChatResult? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        val emails = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)?.toList()
            ?: return null
        if (emails.isEmpty()) return null
        val groupSettings = if (emails.size > 1) {
            NewChatNavKey.NewChatResult.GroupSettings(
                title = intent.getStringExtra(AddContactActivity.EXTRA_CHAT_TITLE),
                isEkr = intent.getBooleanExtra(AddContactActivity.EXTRA_EKR, false),
                isChatLink = intent.getBooleanExtra(AddContactActivity.EXTRA_CHAT_LINK, false),
                allowAddParticipants = intent.getBooleanExtra(
                    AddContactActivity.ALLOW_ADD_PARTICIPANTS,
                    false,
                ),
            )
        } else {
            null
        }
        return NewChatNavKey.NewChatResult(emails = emails, groupSettings = groupSettings)
    }
}
