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
 * contact-row adapter (`MegaContactsAdapter`, `ShareContactsHeaderAdapter`).
 * The adapter sets the composition once in `onCreateViewHolder` and then
 * mutates [uiState], [isSelected] and [onClick] on each bind so the
 * composition stays warm and only the [ContactItemView] subtree recomposes,
 * avoiding the visible flash that comes from re-invoking
 * `ComposeView.setContent` every bind.
 *
 * [isSelected] is owned here — at the Compose layer — instead of being baked
 * into [ContactItemUiState], so ViewModels and mappers stay selection-agnostic
 * (mirrors the `NodeSelectionState` pattern used for Cloud Drive nodes).
 *
 * [onClick] is also owned here because Compose's `combinedClickable` always
 * installs a pointer-input handler that runs `detectTapGestures` and consumes
 * the touch even when its `enabled` flag is `false`. That means any
 * `OnClickListener` set on the host `View` (the row's `RelativeLayout`) is
 * never reached for taps that land on the embedded `ComposeView`. Routing
 * the tap through the Compose layer ensures the click handler actually fires.
 */
class ShareContactRowState {
    var uiState: ContactItemUiState? by mutableStateOf(null)
    var isSelected: Boolean by mutableStateOf(false)
    var onClick: (() -> Unit)? by mutableStateOf(null)
    var onLongClick: (() -> Unit)? by mutableStateOf(null)
}

/**
 * Installs the row composition on [composeView] backed by [state]. Call once
 * per [ComposeView] (typically from `onCreateViewHolder`); subsequent binds
 * should update `state.uiState` / `state.isSelected` / `state.onClick`
 * instead of calling this again.
 */
fun bindShareContactRow(composeView: ComposeView, state: ShareContactRowState) {
    composeView.setContent {
        AndroidTheme(isDark = isSystemInDarkTheme()) {
            state.uiState?.let { uiState ->
                ContactItemView(
                    contactItemUiState = uiState,
                    selected = state.isSelected,
                    onClick = state.onClick,
                    onLongClick = state.onLongClick,
                )
            }
        }
    }
}
