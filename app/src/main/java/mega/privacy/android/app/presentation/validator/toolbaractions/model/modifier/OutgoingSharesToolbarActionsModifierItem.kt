package mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier

import androidx.compose.runtime.Immutable

@Immutable
data class OutgoingSharesToolbarActionsModifierItem(
    val areAllNotTakenDown: Boolean = false,
    val isRootLevel: Boolean = false,
    val shouldHideLink: Boolean = false,
    val addToItem: OutgoingSharesAddToActionModifierItem = OutgoingSharesAddToActionModifierItem(),
)

@Immutable
data class OutgoingSharesAddToActionModifierItem(
    val canBeAddedToAlbum: Boolean = false,
    val canBeAddedTo: Boolean = false,
)
