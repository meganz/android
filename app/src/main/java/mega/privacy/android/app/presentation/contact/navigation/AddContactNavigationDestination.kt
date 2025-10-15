package mega.privacy.android.app.presentation.contact.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.destination.AddContactToShareNavKey

fun EntryProviderScope<NavKey>.addContactLegacyDestination(
    returnResult: (String, List<String>?) -> Unit,
) {
    entry<AddContactToShareNavKey> { addContactToShare ->
        val launcher = rememberLauncherForActivityResult(
            contract = AddContactsContract()
        ) { result ->
            returnResult(AddContactToShareNavKey.KEY, result?.emails)
        }

        LaunchedEffect(Unit) {
            launcher.launch(addContactToShare)
        }
    }
}

