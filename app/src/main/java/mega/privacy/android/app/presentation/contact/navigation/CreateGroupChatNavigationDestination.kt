package mega.privacy.android.app.presentation.contact.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey

/**
 * Registers the legacy "Add contacts → create group chat" activity as a destination.
 *
 * Mirrors [addContactLegacyDestination]: on entry, launches [AddContactActivity] with the
 * "only create group" extras, then forwards the parsed [NewGroupChatResult]
 * through [returnResult] under [CreateGroupChatNavKey.KEY].
 */
fun EntryProviderScope<NavKey>.createGroupChatLegacyDestination(
    returnResult: (String, CreateGroupChatNavKey.NewGroupChatResult?) -> Unit,
) {
    entry<CreateGroupChatNavKey> {
        val launcher = rememberLauncherForActivityResult(
            contract = CreateGroupChatContract(),
        ) { selection ->
            returnResult(CreateGroupChatNavKey.KEY, selection)
        }
        LaunchedEffect(Unit) {
            launcher.launch(Unit)
        }
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
