package mega.privacy.android.app.presentation.contact.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import mega.privacy.android.navigation.destination.AddContactToShare

fun NavGraphBuilder.addContactLegacyDestination(
    returnResult: (String, List<String>?) -> Unit,
) {
    composable<AddContactToShare> {
        val addContactToShare = it.toRoute<AddContactToShare>()
        val launcher = rememberLauncherForActivityResult(
            contract = AddContactsContract()
        ) { result ->
            returnResult(AddContactToShare.KEY, result?.emails)
        }

        LaunchedEffect(Unit) {
            launcher.launch(addContactToShare)
        }
    }
}

