package mega.privacy.android.app.presentation.contact.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.feature.contact.navigation.AddContactToShareEntry
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.AddContactToShareNavKey

/**
 * Registers the [AddContactToShareNavKey] destination. Behind [AppFeatures.ContactsComposeUI]
 * either renders the Compose [AddContactToShareEntry] picker (flag on) or launches the legacy
 * [AddContactActivity] (flag off). Both paths publish the selected contact emails as a
 * `List<String>` under [AddContactToShareNavKey.KEY].
 */
fun EntryProviderScope<NavKey>.addContactLegacyDestination(navigationHandler: NavigationHandler) {
    entry<AddContactToShareNavKey> { addContactToShare ->
        FeatureFlagGate(
            feature = AppFeatures.ContactsComposeUI,
            disabled = {
                LegacyAddContactToShareEntry(
                    addContactToShare = addContactToShare,
                    onResult = { emails ->
                        navigationHandler.returnResult(AddContactToShareNavKey.KEY, emails)
                    },
                )
            },
            enabled = {
                AddContactToShareEntry(navigationHandler = navigationHandler)
            },
        )
    }
}

/**
 * Launches the legacy [AddContactActivity] via [AddContactsContract] and forwards the selected
 * emails (null when cancelled) through [onResult], which publishes them under the nav key.
 */
@Composable
private fun LegacyAddContactToShareEntry(
    addContactToShare: AddContactToShareNavKey,
    onResult: (List<String>?) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = AddContactsContract(),
    ) { result ->
        onResult(result?.emails)
    }
    LaunchedEffect(Unit) {
        launcher.launch(addContactToShare)
    }
}
