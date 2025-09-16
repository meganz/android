package mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier

import androidx.compose.runtime.Immutable

@Immutable
data class CloudDriveSyncsToolbarActionsModifierItem(
    val hiddenNodeItem: CloudDriveSyncsHiddenNodeActionModifierItem = CloudDriveSyncsHiddenNodeActionModifierItem(),
    val favouritesItem: CloudDriveSyncsFavouritesActionModifierItem = CloudDriveSyncsFavouritesActionModifierItem(),
    val addToItem: CloudDriveSyncsAddToActionModifierItem = CloudDriveSyncsAddToActionModifierItem(),
    val addLabelItem: CloudDriveSyncsAddLabelActionModifierItem = CloudDriveSyncsAddLabelActionModifierItem(),
)

@Immutable
data class CloudDriveSyncsHiddenNodeActionModifierItem(
    val isEnabled: Boolean = false,
    val canBeHidden: Boolean = false,
)

@Immutable
data class CloudDriveSyncsAddToActionModifierItem(
    val canBeAddedToAlbum: Boolean = false,
    val canBeAddedTo: Boolean = false,
)

@Immutable
data class CloudDriveSyncsFavouritesActionModifierItem(
    val canBeAdded: Boolean = false,
    val canBeRemoved: Boolean = false,
)

@Immutable
data class CloudDriveSyncsAddLabelActionModifierItem(
    val canBeAdded: Boolean = false,
)
