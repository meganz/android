package mega.privacy.android.feature.contact.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.contact.add.AddContactViewModel
import mega.privacy.android.feature.contact.add.view.AddContactsScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.AddContactsNavKey

fun EntryProviderScope<NavKey>.addContacts(navigationHandler: NavigationHandler) {
    entry<AddContactsNavKey>() {
        val viewmodel: AddContactViewModel = hiltViewModel()
        val state by viewmodel.uiState.collectAsStateWithLifecycle()
        AddContactsScreen(state = state)
    }
}