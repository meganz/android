package mega.privacy.android.core.nodecomponents.action.eventhandler.mapper

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.AddToAlbumMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.FavouriteMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.InfoMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.LabelMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RenameMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareFolderMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SyncMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier
import mega.privacy.mobile.analytics.event.AddToAlbumMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveCopyMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveDownloadMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveFavouriteMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveHideMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveInfoMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveLabelMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveMoveMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveMoveToRubbishBinMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveRenameMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveShareFolderMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveShareLinkMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveSyncMenuItemEvent
import javax.inject.Inject

class CloudDriveActionEventMapper @Inject constructor() {
    operator fun invoke(menuAction: MenuAction): EventIdentifier? {
        return when (menuAction) {
            is InfoMenuAction -> CloudDriveInfoMenuItemEvent
            is FavouriteMenuAction -> CloudDriveFavouriteMenuItemEvent
            is LabelMenuAction -> CloudDriveLabelMenuItemEvent
            is AvailableOfflineMenuAction -> CloudDriveDownloadMenuItemEvent
            is ManageLinkMenuAction -> CloudDriveShareLinkMenuItemEvent
            is ShareFolderMenuAction -> CloudDriveShareFolderMenuItemEvent
            is RenameMenuAction -> CloudDriveRenameMenuItemEvent
            is HideMenuAction -> CloudDriveHideMenuItemEvent
            is AddToAlbumMenuAction -> AddToAlbumMenuItemEvent
            is MoveMenuAction -> CloudDriveMoveMenuItemEvent
            is CopyMenuAction -> CloudDriveCopyMenuItemEvent
            is TrashMenuAction -> CloudDriveMoveToRubbishBinMenuItemEvent
            is SyncMenuAction -> CloudDriveSyncMenuItemEvent
            else -> null

        }
    }
}