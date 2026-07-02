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
import mega.privacy.android.feature.contact.group.create.navigation.CreateGroupChatEntry
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey

/**
 * Registers the [CreateGroupChatNavKey] destination. Behind [AppFeatures.ContactsComposeUI] either
 * renders the Compose [CreateGroupChatEntry] screen (flag on) or launches the legacy
 * [AddContactActivity] in "only create group" mode (flag off). Both paths publish the chosen
 * [CreateGroupChatNavKey.NewGroupChatResult] under [CreateGroupChatNavKey.KEY]; the consuming caller
 * performs the actual group creation.
 *
 * TODO: Move this entry to the feature module once the feature flag is removed
 */
fun EntryProviderScope<NavKey>.createGroupChatLegacyDestination(navigationHandler: NavigationHandler) {
    entry<CreateGroupChatNavKey> {
        FeatureFlagGate(
            feature = AppFeatures.ContactsComposeUI,
            disabled = {
                LegacyCreateGroupChatEntry(
                    onResult = { result ->
                        navigationHandler.returnResult(CreateGroupChatNavKey.KEY, result)
                    },
                )
            },
            enabled = {
                CreateGroupChatEntry(navigationHandler = navigationHandler)
            },
        )
    }
}

/**
 * Launches the legacy [AddContactActivity] in "only create group" mode and forwards the parsed
 * [CreateGroupChatNavKey.NewGroupChatResult] (null when cancelled) through [onResult].
 */
@Composable
private fun LegacyCreateGroupChatEntry(
    onResult: (CreateGroupChatNavKey.NewGroupChatResult?) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = CreateGroupChatContract(),
    ) { result ->
        onResult(result)
    }
    LaunchedEffect(Unit) {
        launcher.launch(Unit)
    }
}

private class CreateGroupChatContract :
    ActivityResultContract<Unit, CreateGroupChatNavKey.NewGroupChatResult?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(context, AddContactActivity::class.java)
            .putExtra(AddContactActivity.EXTRA_CONTACT_TYPE, Constants.CONTACT_TYPE_MEGA)
            .putExtra(AddContactActivity.EXTRA_ONLY_CREATE_GROUP, true)

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): CreateGroupChatNavKey.NewGroupChatResult? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        val emails = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)?.toList()
            ?: return null
        if (emails.isEmpty()) return null
        return CreateGroupChatNavKey.NewGroupChatResult(
            emails = emails,
            title = intent.getStringExtra(AddContactActivity.EXTRA_CHAT_TITLE),
            isEkr = intent.getBooleanExtra(AddContactActivity.EXTRA_EKR, false),
            isChatLink = intent.getBooleanExtra(AddContactActivity.EXTRA_CHAT_LINK, false),
            allowAddParticipants = intent.getBooleanExtra(
                AddContactActivity.ALLOW_ADD_PARTICIPANTS,
                false,
            ),
        )
    }
}
