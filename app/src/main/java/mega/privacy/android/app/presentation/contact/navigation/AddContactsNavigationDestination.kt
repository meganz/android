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
import mega.privacy.android.feature.contact.navigation.AddContactsEntry
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.AddContactsNavKey

/**
 * Registers the [AddContactsNavKey] destination. Behind [AppFeatures.ContactsComposeUI] either
 * renders the Compose [AddContactsEntry] MEGA-contacts picker (flag on) or launches the legacy
 * [AddContactActivity] in MEGA-contacts mode (flag off). Both paths publish the selected contact
 * emails as a `List<String>` under [AddContactsNavKey.KEY].
 *
 * TODO: Move this entry to the feature module once the feature flag is removed
 */
fun EntryProviderScope<NavKey>.addContactsDestination(navigationHandler: NavigationHandler) {
    entry<AddContactsNavKey> {
        FeatureFlagGate(
            feature = AppFeatures.ContactsComposeUI,
            disabled = {
                LegacyAddContactsEntry(
                    onResult = { emails ->
                        navigationHandler.returnResult(AddContactsNavKey.KEY, emails)
                    },
                )
            },
            enabled = {
                AddContactsEntry(navigationHandler = navigationHandler)
            },
        )
    }
}

/**
 * Launches the legacy [AddContactActivity] in MEGA-contacts mode and forwards the selected emails
 * (empty when cancelled) through [onResult], which publishes them and pops the entry.
 */
@Composable
private fun LegacyAddContactsEntry(onResult: (List<String>) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = AttachMegaContactsContract(),
    ) { emails ->
        onResult(emails.orEmpty())
    }
    LaunchedEffect(Unit) {
        launcher.launch(Unit)
    }
}

private class AttachMegaContactsContract : ActivityResultContract<Unit, List<String>?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(context, AddContactActivity::class.java)
            .putExtra(Constants.INTENT_EXTRA_KEY_CONTACT_TYPE, Constants.CONTACT_TYPE_MEGA)
            .putExtra(Constants.INTENT_EXTRA_KEY_CHAT, true)
            .putExtra(
                Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
                context.getString(R.string.send_contacts),
            )

    override fun parseResult(resultCode: Int, intent: Intent?): List<String>? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        return intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)?.toList()
    }
}
