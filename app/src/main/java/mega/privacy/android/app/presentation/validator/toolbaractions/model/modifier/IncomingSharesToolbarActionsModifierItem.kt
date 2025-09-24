package mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier

import androidx.compose.runtime.Immutable

@Immutable
data class IncomingSharesToolbarActionsModifierItem(
    val renameItem: IncomingSharesRenameActionModifierItem = IncomingSharesRenameActionModifierItem(),
    val moveItem: IncomingSharesMoveActionModifierItem = IncomingSharesMoveActionModifierItem(),
    val copyItem: IncomingSharesCopyActionModifierItem = IncomingSharesCopyActionModifierItem(),
)

@Immutable
data class IncomingSharesRenameActionModifierItem(
    val isEnabled: Boolean = false,
)

@Immutable
data class IncomingSharesMoveActionModifierItem(
    val isEnabled: Boolean = false,
)

@Immutable
data class IncomingSharesCopyActionModifierItem(
    val isEnabled: Boolean = false,
)
