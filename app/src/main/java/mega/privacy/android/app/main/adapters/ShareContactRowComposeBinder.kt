package mega.privacy.android.app.main.adapters

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.shared.contact.components.ContactItemView
import mega.privacy.android.shared.contact.model.ContactItemUiState

/**
 * Stable per-row state holder for the embedded [ComposeView] in a
 * `ShareContactsHeaderAdapter` row. The adapter sets the composition once in
 * `onCreateViewHolder` and then mutates [uiState] on each bind so the
 * composition stays warm and only the [ContactItemView] subtree recomposes,
 * avoiding the visible flash that comes from re-invoking
 * `ComposeView.setContent` every bind.
 */
class ShareContactRowState {
    var uiState: ContactItemUiState? by mutableStateOf(null)
}

/**
 * Installs the row composition on [composeView] backed by [state]. Call once
 * per [ComposeView] (typically from `onCreateViewHolder`); subsequent binds
 * should update `state.uiState` instead of calling this again.
 */
fun bindShareContactRow(composeView: ComposeView, state: ShareContactRowState) {
    composeView.setContent {
        AndroidTheme(isDark = isSystemInDarkTheme()) {
            state.uiState?.let { uiState ->
                ContactItemView(
                    contactItemUiState = uiState,
                    showDivider = false,
                )
            }
        }
    }
}
