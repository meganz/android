package mega.privacy.android.core.nodecomponents.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.entity.NodeBottomSheetMenuItem
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
import mega.privacy.android.core.nodecomponents.menu.menuitem.RemoveFavouriteBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.RemoveLinkBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.RemoveShareBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.RenameBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.RestoreBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.SendToChatBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.ShareBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.ShareFolderBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.SlideshowBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.TrashBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.UnhideBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.VerifyBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.VersionsBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.ViewInFolderBottomSheetMenuItem
import mega.privacy.android.domain.qualifier.features.Backups
import mega.privacy.android.domain.qualifier.features.CloudDrive
import mega.privacy.android.domain.qualifier.features.IncomingShares
import mega.privacy.android.domain.qualifier.features.Links
import mega.privacy.android.domain.qualifier.features.OutgoingShares
import mega.privacy.android.domain.qualifier.features.RubbishBin
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
         * Provide cloudDrive toolbar options
         */
        @Provides
        @ElementsIntoSet
        @CloudDrive
        @Singleton
        fun provideCloudDriveBottomSheetOptions(
            availableOfflineMenuAction: AvailableOfflineBottomSheetMenuItem,
            copyMenuAction: CopyBottomSheetMenuItem,
            deletePermanentlyMenuAction: DeletePermanentlyBottomSheetMenuItem,
            disputeTakeDownMenuAction: DisputeTakeDownBottomSheetMenuItem,
            downloadMenuAction: DownloadBottomSheetMenuItem,
            editMenuAction: EditBottomSheetMenuItem,
            favouriteMenuAction: FavouriteBottomSheetMenuItem,
            removeFavouriteMenuAction: RemoveFavouriteBottomSheetMenuItem,
            getLinkMenuAction: GetLinkBottomSheetMenuItem,
            infoMenuAction: InfoBottomSheetMenuItem,
            labelMenuAction: LabelBottomSheetMenuItem,
            leaveShareMenuAction: LeaveShareBottomSheetMenuItem,
            manageLinkMenuAction: ManageLinkBottomSheetMenuItem,
            manageShareFolderBottomSheetMenuItem: ManageShareFolderBottomSheetMenuItem,
            moveMenuAction: MoveBottomSheetMenuItem,
            openLocationMenuAction: OpenLocationBottomSheetMenuItem,
            openWithMenuAction: OpenWithBottomSheetMenuItem,
            removeLinkMenuAction: RemoveLinkBottomSheetMenuItem,
            removeShareMenuAction: RemoveShareBottomSheetMenuItem,
            renameMenuAction: RenameBottomSheetMenuItem,
            hideMenuAction: HideBottomSheetMenuItem,
            unhideMenuAction: UnhideBottomSheetMenuItem,
            restoreMenuAction: RestoreBottomSheetMenuItem,
            sendToChatMenuAction: SendToChatBottomSheetMenuItem,
            shareMenuAction: ShareBottomSheetMenuItem,
            shareFolderMenuAction: ShareFolderBottomSheetMenuItem,
            slideshowMenuAction: SlideshowBottomSheetMenuItem,
            trashMenuAction: TrashBottomSheetMenuItem,
            verifyMenuAction: VerifyBottomSheetMenuItem,
            versionsMenuAction: VersionsBottomSheetMenuItem,
            viewInFolderMenuAction: ViewInFolderBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return setOf(
                availableOfflineMenuAction,
                copyMenuAction,
                deletePermanentlyMenuAction,
                disputeTakeDownMenuAction,
                downloadMenuAction,
                editMenuAction,
                favouriteMenuAction,
                removeFavouriteMenuAction,
                getLinkMenuAction,
                infoMenuAction,
                labelMenuAction,
                leaveShareMenuAction,
                manageLinkMenuAction,
                manageShareFolderBottomSheetMenuItem,
                moveMenuAction,
                openLocationMenuAction,
                openWithMenuAction,
                removeLinkMenuAction,
                removeShareMenuAction,
                renameMenuAction,
                hideMenuAction,
                unhideMenuAction,
                restoreMenuAction,
                sendToChatMenuAction,
                shareMenuAction,
                shareFolderMenuAction,
                slideshowMenuAction,
                trashMenuAction,
                verifyMenuAction,
                versionsMenuAction,
                viewInFolderMenuAction,
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
            availableOfflineMenuAction: AvailableOfflineBottomSheetMenuItem,
            copyMenuAction: CopyBottomSheetMenuItem,
            deletePermanentlyMenuAction: DeletePermanentlyBottomSheetMenuItem,
            disputeTakeDownMenuAction: DisputeTakeDownBottomSheetMenuItem,
            downloadMenuAction: DownloadBottomSheetMenuItem,
            editMenuAction: EditBottomSheetMenuItem,
            favouriteMenuAction: FavouriteBottomSheetMenuItem,
            removeFavouriteMenuAction: RemoveFavouriteBottomSheetMenuItem,
            getLinkMenuAction: GetLinkBottomSheetMenuItem,
            infoMenuAction: InfoBottomSheetMenuItem,
            labelMenuAction: LabelBottomSheetMenuItem,
            leaveShareMenuAction: LeaveShareBottomSheetMenuItem,
            manageLinkMenuAction: ManageLinkBottomSheetMenuItem,
            manageShareFolderBottomSheetMenuItem: ManageShareFolderBottomSheetMenuItem,
            moveMenuAction: MoveBottomSheetMenuItem,
            openLocationMenuAction: OpenLocationBottomSheetMenuItem,
            openWithMenuAction: OpenWithBottomSheetMenuItem,
            removeLinkMenuAction: RemoveLinkBottomSheetMenuItem,
            removeShareMenuAction: RemoveShareBottomSheetMenuItem,
            renameMenuAction: RenameBottomSheetMenuItem,
            restoreMenuAction: RestoreBottomSheetMenuItem,
            sendToChatMenuAction: SendToChatBottomSheetMenuItem,
            shareMenuAction: ShareBottomSheetMenuItem,
            shareFolderMenuAction: ShareFolderBottomSheetMenuItem,
            slideshowMenuAction: SlideshowBottomSheetMenuItem,
            trashMenuAction: TrashBottomSheetMenuItem,
            verifyMenuAction: VerifyBottomSheetMenuItem,
            versionsMenuAction: VersionsBottomSheetMenuItem,
            viewInFolderMenuAction: ViewInFolderBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return setOf(
                availableOfflineMenuAction,
                copyMenuAction,
                deletePermanentlyMenuAction,
                disputeTakeDownMenuAction,
                downloadMenuAction,
                editMenuAction,
                favouriteMenuAction,
                removeFavouriteMenuAction,
                getLinkMenuAction,
                infoMenuAction,
                labelMenuAction,
                leaveShareMenuAction,
                manageLinkMenuAction,
                manageShareFolderBottomSheetMenuItem,
                moveMenuAction,
                openLocationMenuAction,
                openWithMenuAction,
                removeLinkMenuAction,
                removeShareMenuAction,
                renameMenuAction,
                restoreMenuAction,
                sendToChatMenuAction,
                shareMenuAction,
                shareFolderMenuAction,
                slideshowMenuAction,
                trashMenuAction,
                verifyMenuAction,
                versionsMenuAction,
                viewInFolderMenuAction,
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
            availableOfflineMenuAction: AvailableOfflineBottomSheetMenuItem,
            copyMenuAction: CopyBottomSheetMenuItem,
            deletePermanentlyMenuAction: DeletePermanentlyBottomSheetMenuItem,
            disputeTakeDownMenuAction: DisputeTakeDownBottomSheetMenuItem,
            downloadMenuAction: DownloadBottomSheetMenuItem,
            editMenuAction: EditBottomSheetMenuItem,
            favouriteMenuAction: FavouriteBottomSheetMenuItem,
            removeFavouriteMenuAction: RemoveFavouriteBottomSheetMenuItem,
            getLinkMenuAction: GetLinkBottomSheetMenuItem,
            infoMenuAction: InfoBottomSheetMenuItem,
            labelMenuAction: LabelBottomSheetMenuItem,
            leaveShareMenuAction: LeaveShareBottomSheetMenuItem,
            manageLinkMenuAction: ManageLinkBottomSheetMenuItem,
            manageShareFolderBottomSheetMenuItem: ManageShareFolderBottomSheetMenuItem,
            moveMenuAction: MoveBottomSheetMenuItem,
            openLocationMenuAction: OpenLocationBottomSheetMenuItem,
            openWithMenuAction: OpenWithBottomSheetMenuItem,
            removeLinkMenuAction: RemoveLinkBottomSheetMenuItem,
            removeShareMenuAction: RemoveShareBottomSheetMenuItem,
            renameMenuAction: RenameBottomSheetMenuItem,
            restoreMenuAction: RestoreBottomSheetMenuItem,
            sendToChatMenuAction: SendToChatBottomSheetMenuItem,
            shareMenuAction: ShareBottomSheetMenuItem,
            shareFolderMenuAction: ShareFolderBottomSheetMenuItem,
            slideshowMenuAction: SlideshowBottomSheetMenuItem,
            trashMenuAction: TrashBottomSheetMenuItem,
            verifyMenuAction: VerifyBottomSheetMenuItem,
            versionsMenuAction: VersionsBottomSheetMenuItem,
            viewInFolderMenuAction: ViewInFolderBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return setOf(
                availableOfflineMenuAction,
                copyMenuAction,
                deletePermanentlyMenuAction,
                disputeTakeDownMenuAction,
                downloadMenuAction,
                editMenuAction,
                favouriteMenuAction,
                removeFavouriteMenuAction,
                getLinkMenuAction,
                infoMenuAction,
                labelMenuAction,
                leaveShareMenuAction,
                manageLinkMenuAction,
                manageShareFolderBottomSheetMenuItem,
                moveMenuAction,
                openLocationMenuAction,
                openWithMenuAction,
                removeLinkMenuAction,
                removeShareMenuAction,
                renameMenuAction,
                restoreMenuAction,
                sendToChatMenuAction,
                shareMenuAction,
                shareFolderMenuAction,
                slideshowMenuAction,
                trashMenuAction,
                verifyMenuAction,
                versionsMenuAction,
                viewInFolderMenuAction,
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
            availableOfflineMenuAction: AvailableOfflineBottomSheetMenuItem,
            copyMenuAction: CopyBottomSheetMenuItem,
            deletePermanentlyMenuAction: DeletePermanentlyBottomSheetMenuItem,
            disputeTakeDownMenuAction: DisputeTakeDownBottomSheetMenuItem,
            downloadMenuAction: DownloadBottomSheetMenuItem,
            editMenuAction: EditBottomSheetMenuItem,
            favouriteMenuAction: FavouriteBottomSheetMenuItem,
            removeFavouriteMenuAction: RemoveFavouriteBottomSheetMenuItem,
            getLinkMenuAction: GetLinkBottomSheetMenuItem,
            infoMenuAction: InfoBottomSheetMenuItem,
            labelMenuAction: LabelBottomSheetMenuItem,
            leaveShareMenuAction: LeaveShareBottomSheetMenuItem,
            manageLinkMenuAction: ManageLinkBottomSheetMenuItem,
            manageShareFolderBottomSheetMenuItem: ManageShareFolderBottomSheetMenuItem,
            moveMenuAction: MoveBottomSheetMenuItem,
            openLocationMenuAction: OpenLocationBottomSheetMenuItem,
            openWithMenuAction: OpenWithBottomSheetMenuItem,
            removeLinkMenuAction: RemoveLinkBottomSheetMenuItem,
            removeShareMenuAction: RemoveShareBottomSheetMenuItem,
            renameMenuAction: RenameBottomSheetMenuItem,
            restoreMenuAction: RestoreBottomSheetMenuItem,
            sendToChatMenuAction: SendToChatBottomSheetMenuItem,
            shareMenuAction: ShareBottomSheetMenuItem,
            shareFolderMenuAction: ShareFolderBottomSheetMenuItem,
            slideshowMenuAction: SlideshowBottomSheetMenuItem,
            trashMenuAction: TrashBottomSheetMenuItem,
            verifyMenuAction: VerifyBottomSheetMenuItem,
            versionsMenuAction: VersionsBottomSheetMenuItem,
            viewInFolderMenuAction: ViewInFolderBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return setOf(
                availableOfflineMenuAction,
                copyMenuAction,
                deletePermanentlyMenuAction,
                disputeTakeDownMenuAction,
                downloadMenuAction,
                editMenuAction,
                favouriteMenuAction,
                removeFavouriteMenuAction,
                getLinkMenuAction,
                infoMenuAction,
                labelMenuAction,
                leaveShareMenuAction,
                manageLinkMenuAction,
                manageShareFolderBottomSheetMenuItem,
                moveMenuAction,
                openLocationMenuAction,
                openWithMenuAction,
                removeLinkMenuAction,
                removeShareMenuAction,
                renameMenuAction,
                restoreMenuAction,
                sendToChatMenuAction,
                shareMenuAction,
                shareFolderMenuAction,
                slideshowMenuAction,
                trashMenuAction,
                verifyMenuAction,
                versionsMenuAction,
                viewInFolderMenuAction,
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
            availableOfflineMenuAction: AvailableOfflineBottomSheetMenuItem,
            copyMenuAction: CopyBottomSheetMenuItem,
            deletePermanentlyMenuAction: DeletePermanentlyBottomSheetMenuItem,
            disputeTakeDownMenuAction: DisputeTakeDownBottomSheetMenuItem,
            downloadMenuAction: DownloadBottomSheetMenuItem,
            editMenuAction: EditBottomSheetMenuItem,
            favouriteMenuAction: FavouriteBottomSheetMenuItem,
            removeFavouriteMenuAction: RemoveFavouriteBottomSheetMenuItem,
            getLinkMenuAction: GetLinkBottomSheetMenuItem,
            infoMenuAction: InfoBottomSheetMenuItem,
            labelMenuAction: LabelBottomSheetMenuItem,
            leaveShareMenuAction: LeaveShareBottomSheetMenuItem,
            manageLinkMenuAction: ManageLinkBottomSheetMenuItem,
            manageShareFolderBottomSheetMenuItem: ManageShareFolderBottomSheetMenuItem,
            moveMenuAction: MoveBottomSheetMenuItem,
            openLocationMenuAction: OpenLocationBottomSheetMenuItem,
            openWithMenuAction: OpenWithBottomSheetMenuItem,
            removeLinkMenuAction: RemoveLinkBottomSheetMenuItem,
            removeShareMenuAction: RemoveShareBottomSheetMenuItem,
            renameMenuAction: RenameBottomSheetMenuItem,
            hideMenuAction: HideBottomSheetMenuItem,
            unhideMenuAction: UnhideBottomSheetMenuItem,
            restoreMenuAction: RestoreBottomSheetMenuItem,
            sendToChatMenuAction: SendToChatBottomSheetMenuItem,
            shareMenuAction: ShareBottomSheetMenuItem,
            shareFolderMenuAction: ShareFolderBottomSheetMenuItem,
            slideshowMenuAction: SlideshowBottomSheetMenuItem,
            trashMenuAction: TrashBottomSheetMenuItem,
            verifyMenuAction: VerifyBottomSheetMenuItem,
            versionsMenuAction: VersionsBottomSheetMenuItem,
            viewInFolderMenuAction: ViewInFolderBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return setOf(
                availableOfflineMenuAction,
                copyMenuAction,
                deletePermanentlyMenuAction,
                disputeTakeDownMenuAction,
                downloadMenuAction,
                editMenuAction,
                favouriteMenuAction,
                removeFavouriteMenuAction,
                getLinkMenuAction,
                infoMenuAction,
                labelMenuAction,
                leaveShareMenuAction,
                manageLinkMenuAction,
                manageShareFolderBottomSheetMenuItem,
                moveMenuAction,
                openLocationMenuAction,
                openWithMenuAction,
                removeLinkMenuAction,
                removeShareMenuAction,
                renameMenuAction,
                hideMenuAction,
                unhideMenuAction,
                restoreMenuAction,
                sendToChatMenuAction,
                shareMenuAction,
                shareFolderMenuAction,
                slideshowMenuAction,
                trashMenuAction,
                verifyMenuAction,
                versionsMenuAction,
                viewInFolderMenuAction,
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
            availableOfflineMenuAction: AvailableOfflineBottomSheetMenuItem,
            copyMenuAction: CopyBottomSheetMenuItem,
            deletePermanentlyMenuAction: DeletePermanentlyBottomSheetMenuItem,
            disputeTakeDownMenuAction: DisputeTakeDownBottomSheetMenuItem,
            downloadMenuAction: DownloadBottomSheetMenuItem,
            editMenuAction: EditBottomSheetMenuItem,
            favouriteMenuAction: FavouriteBottomSheetMenuItem,
            removeFavouriteMenuAction: RemoveFavouriteBottomSheetMenuItem,
            getLinkMenuAction: GetLinkBottomSheetMenuItem,
            infoMenuAction: InfoBottomSheetMenuItem,
            labelMenuAction: LabelBottomSheetMenuItem,
            leaveShareMenuAction: LeaveShareBottomSheetMenuItem,
            manageLinkMenuAction: ManageLinkBottomSheetMenuItem,
            manageShareFolderBottomSheetMenuItem: ManageShareFolderBottomSheetMenuItem,
            moveMenuAction: MoveBottomSheetMenuItem,
            openLocationMenuAction: OpenLocationBottomSheetMenuItem,
            openWithMenuAction: OpenWithBottomSheetMenuItem,
            removeLinkMenuAction: RemoveLinkBottomSheetMenuItem,
            removeShareMenuAction: RemoveShareBottomSheetMenuItem,
            renameMenuAction: RenameBottomSheetMenuItem,
            hideMenuAction: HideBottomSheetMenuItem,
            unhideMenuAction: UnhideBottomSheetMenuItem,
            restoreMenuAction: RestoreBottomSheetMenuItem,
            sendToChatMenuAction: SendToChatBottomSheetMenuItem,
            shareMenuAction: ShareBottomSheetMenuItem,
            shareFolderMenuAction: ShareFolderBottomSheetMenuItem,
            slideshowMenuAction: SlideshowBottomSheetMenuItem,
            trashMenuAction: TrashBottomSheetMenuItem,
            verifyMenuAction: VerifyBottomSheetMenuItem,
            versionsMenuAction: VersionsBottomSheetMenuItem,
            viewInFolderMenuAction: ViewInFolderBottomSheetMenuItem,
        ): Set<NodeBottomSheetMenuItem<MenuActionWithIcon>> {
            return setOf(
                availableOfflineMenuAction,
                copyMenuAction,
                deletePermanentlyMenuAction,
                disputeTakeDownMenuAction,
                downloadMenuAction,
                editMenuAction,
                favouriteMenuAction,
                removeFavouriteMenuAction,
                getLinkMenuAction,
                infoMenuAction,
                labelMenuAction,
                leaveShareMenuAction,
                manageLinkMenuAction,
                manageShareFolderBottomSheetMenuItem,
                moveMenuAction,
                openLocationMenuAction,
                openWithMenuAction,
                removeLinkMenuAction,
                removeShareMenuAction,
                renameMenuAction,
                hideMenuAction,
                unhideMenuAction,
                restoreMenuAction,
                sendToChatMenuAction,
                shareMenuAction,
                shareFolderMenuAction,
                slideshowMenuAction,
                trashMenuAction,
                verifyMenuAction,
                versionsMenuAction,
                viewInFolderMenuAction,
            )
        }
    }
}