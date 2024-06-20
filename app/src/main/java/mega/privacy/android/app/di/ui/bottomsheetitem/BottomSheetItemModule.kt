package mega.privacy.android.app.di.ui.bottomsheetitem

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.Backups
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.CloudDrive
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.IncomingShares
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.Links
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.OutgoingShares
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.RubbishBin
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.AvailableOfflineBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.CopyBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.DeletePermanentlyBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.DisputeTakeDownBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.DownloadBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.EditBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.FavouriteBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.GetLinkBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.HideBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.InfoBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.LabelBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.LeaveShareBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.ManageLinkBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.ManageShareFolderBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.MoveBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.NodeBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.OpenLocationBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.OpenWithBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.RemoveFavouriteBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.RemoveLinkBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.RemoveShareBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.RenameBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.RestoreBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.SendToChatBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.ShareBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.ShareFolderBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.SlideshowBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.TrashBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.UnhideBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.VerifyBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.VersionsBottomSheetMenuItem
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.ViewInFolderBottomSheetMenuItem
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon

/**
 * Toolbar module
 *
 * Handles the toolbar option inject based on the selected screen
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BottomSheetItemModule {

    companion object {

        /**
         * Provide cloudDrive toolbar options
         */
        @Provides
        @ElementsIntoSet
        @CloudDrive
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