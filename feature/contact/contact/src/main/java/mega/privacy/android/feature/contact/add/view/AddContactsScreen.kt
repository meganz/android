package mega.privacy.android.feature.contact.add.view

import androidx.compose.runtime.Composable
import mega.privacy.android.feature.contact.add.model.AddContactUiState

@Composable
internal fun AddContactsScreen(
    state: AddContactUiState,
) {
    when (state) {
        AddContactUiState.Loading -> {
            // Loading UI
        }

        AddContactUiState.Data -> {
            // Data UI
        }
    }
}