package mega.privacy.android.core.nodecomponents.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuitem.AddToAlbumBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.AddToBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.AddToPlaylistBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.AvailableOfflineBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.CopyBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.DeletePermanentlyBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.DisputeTakeDownBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.DownloadBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.EditBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.FavouriteBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.GetLinkBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.HideBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.InfoBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.LabelBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.LeaveShareBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.ManageLinkBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.ManageShareFolderBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.MoveBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.OpenLocationBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.OpenWithBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.RemoveAvailableOfflineBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.RemoveFavouriteBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.RemoveLinkBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.RemoveRecentlyWatchedVideoBottomSheetItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.RemoveShareBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.RenameBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.RestoreBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.SaveToMegaBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.SendToChatBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.ShareBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.ShareFolderBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.SlideshowBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.SyncBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.TrashBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.UnhideBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.VerifyBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.VersionsBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.ViewInFolderBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.qualifier.features.Backups
import mega.privacy.android.domain.qualifier.features.CloudDrive
import mega.privacy.android.domain.qualifier.features.FolderLink
import mega.privacy.android.domain.qualifier.features.IncomingShares
import mega.privacy.android.domain.qualifier.features.Links
import mega.privacy.android.domain.qualifier.features.OutgoingShares
import mega.privacy.android.domain.qualifier.features.RubbishBin
import mega.privacy.android.domain.qualifier.features.VideoPlaylist
import mega.privacy.android.domain.qualifier.features.VideoRecentlyWatched
import mega.privacy.android.domain.qualifier.features.Videos
import javax.inject.Singleton

/**
 * Toolbar module
 *
 * Handles the toolbar option inject based on the selected screen
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NodeActionsBottomSheetModule {

    companion object {

        /**
         * Provide bottom sheet menu items shared across all node source sections
         */
        @Provides
        @BaseShareMenuItems
        @Singleton
        fun provideBaseShareMenuItems(
            availableOfflineMenuAction: AvailableOfflineBottomSheetMenuItem,
            removeOfflineMenuAction: RemoveAvailableOfflineBottomSheetMenuItem,
            copyMenuAction: CopyBottomSheetMenuItem,
            deletePermanentlyMenuAction: DeletePermanentlyBottomSheetMenuItem,
            disputeTakeDownMenuAction: DisputeTakeDownBottomSheetMenuItem,
            downloadMenuAction: DownloadBottomSheetMenuItem,
            favouriteMenuAction: FavouriteBottomSheetMenuItem,
            removeFavouriteMenuAction: RemoveFavouriteBottomSheetMenuItem,
            getLinkMenuAction: GetLinkBottomSheetMenuItem,
            infoMenuAction: InfoBottomSheetMenuItem,
            labelMenuAction: LabelBottomSheetMenuItem,
            leaveShareMenuAction: LeaveShareBottomSheetMenuItem,
            manageLinkMenuAction: ManageLinkBottomSheetMenuItem,
            manageShareFolderBottomSheetMenuItem: ManageShareFolderBottomSheetMenuItem,
            openLocationMenuAction: OpenLocationBottomSheetMenuItem,
            openWithMenuAction: OpenWithBottomSheetMenuItem,
            removeLinkMenuAction: RemoveLinkBottomSheetMenuItem,
            removeShareMenuAction: RemoveShareBottomSheetMenuItem,
            renameMenuAction: RenameBottomSheetMenuItem,
            restoreMenuAction: RestoreBottomSheetMenuItem,
            sendToChatMenuAction: SendToChatBottomSheetMenuItem,
            shareMenuAction: ShareBottomSheetMenuItem,
            trashMenuAction: TrashBottomSheetMenuItem,
            verifyMenuAction: VerifyBottomSheetMenuItem,
            versionsMenuAction: VersionsBottomSheetMenuItem,
            viewInFolderMenuAction: ViewInFolderBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> = setOf(
            availableOfflineMenuAction,
            removeOfflineMenuAction,
            copyMenuAction,
            deletePermanentlyMenuAction,
            disputeTakeDownMenuAction,
            downloadMenuAction,
            favouriteMenuAction,
            removeFavouriteMenuAction,
            getLinkMenuAction,
            infoMenuAction,
            labelMenuAction,
            leaveShareMenuAction,
            manageLinkMenuAction,
            manageShareFolderBottomSheetMenuItem,
            openLocationMenuAction,
            openWithMenuAction,
            removeLinkMenuAction,
            removeShareMenuAction,
            renameMenuAction,
            restoreMenuAction,
            sendToChatMenuAction,
            shareMenuAction,
            trashMenuAction,
            verifyMenuAction,
            versionsMenuAction,
            viewInFolderMenuAction,
        )

        /**
         * Provide cloudDrive toolbar options
         */
        @Provides
        @ElementsIntoSet
        @CloudDrive
        @Singleton
        fun provideCloudDriveBottomSheetOptions(
            @BaseShareMenuItems baseItems: Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>,
            editMenuAction: EditBottomSheetMenuItem,
            moveMenuAction: MoveBottomSheetMenuItem,
            hideMenuAction: HideBottomSheetMenuItem,
            unhideMenuAction: UnhideBottomSheetMenuItem,
            shareFolderMenuAction: ShareFolderBottomSheetMenuItem,
            slideshowMenuAction: SlideshowBottomSheetMenuItem,
            syncBottomSheetMenuItem: SyncBottomSheetMenuItem,
            addToBottomSheetMenuItem: AddToBottomSheetMenuItem,
            addToAlbumBottomSheetMenuItem: AddToAlbumBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return baseItems + setOf(
                editMenuAction,
                moveMenuAction,
                hideMenuAction,
                unhideMenuAction,
                shareFolderMenuAction,
                slideshowMenuAction,
                syncBottomSheetMenuItem,
                addToBottomSheetMenuItem,
                addToAlbumBottomSheetMenuItem,
            )
        }

        /**
         * Provide outgoing shares toolbar options
         */
        @Provides
        @ElementsIntoSet
        @OutgoingShares
        @Singleton
        fun provideOutgoingSharesBottomSheetOptions(
            @BaseShareMenuItems baseItems: Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>,
            editMenuAction: EditBottomSheetMenuItem,
            shareFolderMenuAction: ShareFolderBottomSheetMenuItem,
            slideshowMenuAction: SlideshowBottomSheetMenuItem,
            addToBottomSheetMenuItem: AddToBottomSheetMenuItem,
            addToAlbumBottomSheetMenuItem: AddToAlbumBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return baseItems + setOf(
                editMenuAction,
                shareFolderMenuAction,
                slideshowMenuAction,
                addToBottomSheetMenuItem,
                addToAlbumBottomSheetMenuItem,
            )
        }

        /**
         * Provide incoming shares toolbar options
         */
        @Provides
        @ElementsIntoSet
        @IncomingShares
        @Singleton
        fun provideIncomingSharesBottomSheetOptions(
            @BaseShareMenuItems baseItems: Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>,
            editMenuAction: EditBottomSheetMenuItem,
            moveMenuAction: MoveBottomSheetMenuItem,
            shareFolderMenuAction: ShareFolderBottomSheetMenuItem,
            slideshowMenuAction: SlideshowBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return baseItems + setOf(
                editMenuAction,
                moveMenuAction,
                shareFolderMenuAction,
                slideshowMenuAction,
            )
        }

        /**
         * Provide links bottom sheet options
         */
        @Provides
        @ElementsIntoSet
        @Links
        @Singleton
        fun provideLinkSharesBottomSheetOptions(
            @BaseShareMenuItems baseItems: Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>,
            editMenuAction: EditBottomSheetMenuItem,
            shareFolderMenuAction: ShareFolderBottomSheetMenuItem,
            slideshowMenuAction: SlideshowBottomSheetMenuItem,
            addToBottomSheetMenuItem: AddToBottomSheetMenuItem,
            addToAlbumBottomSheetMenuItem: AddToAlbumBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return baseItems + setOf(
                editMenuAction,
                shareFolderMenuAction,
                slideshowMenuAction,
                addToBottomSheetMenuItem,
                addToAlbumBottomSheetMenuItem,
            )
        }

        /**
         * Provide rubbish bin bottom sheet options
         */
        @Provides
        @ElementsIntoSet
        @RubbishBin
        @Singleton
        fun provideRubbishBinsBottomSheetOptions(
            deletePermanentlyMenuAction: DeletePermanentlyBottomSheetMenuItem,
            infoMenuAction: InfoBottomSheetMenuItem,
            restoreMenuAction: RestoreBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return setOf(
                deletePermanentlyMenuAction,
                infoMenuAction,
                restoreMenuAction,
            )
        }

        /**
         * Provide backups bottom sheet options
         */
        @Provides
        @ElementsIntoSet
        @Backups
        @Singleton
        fun provideBackupsBottomSheetOptions(
            @BaseShareMenuItems baseItems: Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>,
            editMenuAction: EditBottomSheetMenuItem,
            moveMenuAction: MoveBottomSheetMenuItem,
            hideMenuAction: HideBottomSheetMenuItem,
            unhideMenuAction: UnhideBottomSheetMenuItem,
            shareFolderMenuAction: ShareFolderBottomSheetMenuItem,
            slideshowMenuAction: SlideshowBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return baseItems + setOf(
                editMenuAction,
                moveMenuAction,
                hideMenuAction,
                unhideMenuAction,
                shareFolderMenuAction,
                slideshowMenuAction,
            )
        }

        /**
         * Provide videos bottom sheet options
         */
        @Provides
        @ElementsIntoSet
        @Videos
        @Singleton
        fun provideVideosBottomSheetOptions(
            @BaseShareMenuItems baseItems: Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>,
            editMenuAction: EditBottomSheetMenuItem,
            moveMenuAction: MoveBottomSheetMenuItem,
            hideMenuAction: HideBottomSheetMenuItem,
            unhideMenuAction: UnhideBottomSheetMenuItem,
            shareFolderMenuAction: ShareFolderBottomSheetMenuItem,
            slideshowMenuAction: SlideshowBottomSheetMenuItem,
            addToPlaylistBottomSheetMenuItem: AddToPlaylistBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return baseItems + setOf(
                editMenuAction,
                moveMenuAction,
                hideMenuAction,
                unhideMenuAction,
                shareFolderMenuAction,
                slideshowMenuAction,
                addToPlaylistBottomSheetMenuItem,
            )
        }

        /**
         * Provide video playlists toolbar options
         */
        @Provides
        @ElementsIntoSet
        @VideoPlaylist
        @Singleton
        fun provideVideoPlaylistsBottomSheetOptions(
            @BaseShareMenuItems baseItems: Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>,
            moveMenuAction: MoveBottomSheetMenuItem,
            hideMenuAction: HideBottomSheetMenuItem,
            unhideMenuAction: UnhideBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return baseItems + setOf(
                moveMenuAction,
                hideMenuAction,
                unhideMenuAction,
            )
        }

        /**
         * Provide VideoRecentlyWatched toolbar options
         */
        @Provides
        @ElementsIntoSet
        @VideoRecentlyWatched
        @Singleton
        fun provideVideoRecentlyWatchedBottomSheetOptions(
            @BaseShareMenuItems baseItems: Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>,
            moveMenuAction: MoveBottomSheetMenuItem,
            hideMenuAction: HideBottomSheetMenuItem,
            unhideMenuAction: UnhideBottomSheetMenuItem,
            removeRecentlyWatchedVideoAction: RemoveRecentlyWatchedVideoBottomSheetItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return baseItems + setOf(
                moveMenuAction,
                hideMenuAction,
                unhideMenuAction,
                removeRecentlyWatchedVideoAction,
            )
        }

        /**
         * Provide folder link toolbar options
         */
        @Provides
        @ElementsIntoSet
        @FolderLink
        @Singleton
        fun provideFolderLinkOptions(
            saveToMegaBottomSheetMenuItem: SaveToMegaBottomSheetMenuItem,
            downloadMenuAction: DownloadBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return setOf(
                saveToMegaBottomSheetMenuItem,
                downloadMenuAction,
            )
        }
    }
}
