package mega.privacy.android.app.di.ui.toolbaritem

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
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ClearSelectionToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.CopyToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.DisputeTakeDownMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.DownloadToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.GetLinkToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.LeaveShareToolBarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ManageLinkToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.MoveToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.MultiSelectManageLinkToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.NodeToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveLinkDropDownMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveLinkToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveShareToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveShareDropDown
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RenameDropdownMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RenameToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RestoreToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.SelectAllToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.SendToChatToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ShareToolBarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ShareFolderToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.TrashToolbarMenuItem

/**
 * Toolbar module
 *
 * Handles the toolbar option inject based on the selected screen
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ToolbarItemModule {

    companion object {

        /**
         * Provide outgoing shares toolbar options
         */
        @Provides
        @ElementsIntoSet
        @OutgoingShares
        fun provideOutgoingSharesToolbarItems(
            selectAllToolbarMenuItem: SelectAllToolbarMenuItem,
            clearSelectionToolbarMenuItem: ClearSelectionToolbarMenuItem,
            download: DownloadToolbarMenuItem,
            removeShareToolbarMenuItem: RemoveShareToolbarMenuItem,
            getLinkToolbarMenuItem: GetLinkToolbarMenuItem,
            manageLinkToolbarMenuItem: ManageLinkToolbarMenuItem,
            removeLinkDropDownMenuItem: RemoveLinkDropDownMenuItem,
            sendToChatToolbarMenuItem: SendToChatToolbarMenuItem,
            shareFolderToolbarMenuItem: ShareFolderToolbarMenuItem,
            shareToolbarMenuItem: ShareToolBarMenuItem,
            renameToolbarMenuItem: RenameToolbarMenuItem,
            copyToolbarMenuItem: CopyToolbarMenuItem,
            trashToolbarMenuItem: TrashToolbarMenuItem,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAllToolbarMenuItem,
                clearSelectionToolbarMenuItem,
                download,
                removeShareToolbarMenuItem,
                getLinkToolbarMenuItem,
                manageLinkToolbarMenuItem,
                removeLinkDropDownMenuItem,
                sendToChatToolbarMenuItem,
                shareFolderToolbarMenuItem,
                shareToolbarMenuItem,
                renameToolbarMenuItem,
                copyToolbarMenuItem,
                trashToolbarMenuItem,
            )
        }

        /**
         * Provide incoming shares toolbar options
         */
        @Provides
        @ElementsIntoSet
        @IncomingShares
        fun provideIncomingSharesToolbarItems(
            selectAll: SelectAllToolbarMenuItem,
            clearSelection: ClearSelectionToolbarMenuItem,
            leaveShareToolBarMenuItem: LeaveShareToolBarMenuItem,
            download: DownloadToolbarMenuItem,
            copyToolbarMenuItem: CopyToolbarMenuItem,
            moveToolbarMenuItem: MoveToolbarMenuItem,
            sendToChatToolbarMenuItem: SendToChatToolbarMenuItem,
            renameToolbarMenuItem: RenameToolbarMenuItem,
            trashToolbarMenuItem: TrashToolbarMenuItem,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                download,
                leaveShareToolBarMenuItem,
                moveToolbarMenuItem,
                sendToChatToolbarMenuItem,
                renameToolbarMenuItem,
                copyToolbarMenuItem,
                trashToolbarMenuItem,
            )
        }

        /**
         * Provide shared links toolbar options
         */
        @Provides
        @ElementsIntoSet
        @Links
        fun provideSharedLinksToolbarItems(
            selectAll: SelectAllToolbarMenuItem,
            clearSelection: ClearSelectionToolbarMenuItem,
            download: DownloadToolbarMenuItem,
            getLinkToolbarMenuItem: GetLinkToolbarMenuItem,
            manageLinkToolbarMenuItem: ManageLinkToolbarMenuItem,
            removeLinkToolbarMenuItem: RemoveLinkToolbarMenuItem,
            removeLinkDropDownMenuItem: RemoveLinkDropDownMenuItem,
            shareToolbarMenuItem: ShareToolBarMenuItem,
            sendToChatToolbarMenuItem: SendToChatToolbarMenuItem,
            renameDropdownMenuItem: RenameDropdownMenuItem,
            copyToolbarMenuItem: CopyToolbarMenuItem,
            trashToolbarMenuItem: TrashToolbarMenuItem,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                download,
                getLinkToolbarMenuItem,
                manageLinkToolbarMenuItem,
                removeLinkToolbarMenuItem,
                removeLinkDropDownMenuItem,
                shareToolbarMenuItem,
                sendToChatToolbarMenuItem,
                renameDropdownMenuItem,
                copyToolbarMenuItem,
                trashToolbarMenuItem,
            )
        }

        /**
         * Provide cloud drive toolbar options
         */
        @Provides
        @ElementsIntoSet
        @CloudDrive
        fun provideCloudDriveToolbarItems(
            selectAllToolbarMenuItem: SelectAllToolbarMenuItem,
            clearSelectionToolbarMenuItem: ClearSelectionToolbarMenuItem,
            renameToolbarMenuItem: RenameToolbarMenuItem,
            manageLink: MultiSelectManageLinkToolbarMenuItem,
            getLinkToolbarMenuItem: GetLinkToolbarMenuItem,
            removeLinkDropDownMenuItem: RemoveLinkDropDownMenuItem,
            download: DownloadToolbarMenuItem,
            trashToolbarMenuItem: TrashToolbarMenuItem,
            moveToolbarMenuItem: MoveToolbarMenuItem,
            copyToolbarMenuItem: CopyToolbarMenuItem,
            sendToChatToolbarMenuItem: SendToChatToolbarMenuItem,
            shareFolderToolbarMenuItem: ShareFolderToolbarMenuItem,
            removeShareDropDown: RemoveShareDropDown,
            disputeTakeDownMenuItem: DisputeTakeDownMenuItem,
            share: ShareToolBarMenuItem,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAllToolbarMenuItem,
                clearSelectionToolbarMenuItem,
                download,
                disputeTakeDownMenuItem,
                moveToolbarMenuItem,
                getLinkToolbarMenuItem,
                manageLink,
                removeLinkDropDownMenuItem,
                sendToChatToolbarMenuItem,
                shareFolderToolbarMenuItem,
                share,
                renameToolbarMenuItem,
                copyToolbarMenuItem,
                trashToolbarMenuItem,
                removeShareDropDown,
            )
        }

        /**
         * Provide backups toolbar options
         */
        @Provides
        @ElementsIntoSet
        @Backups
        fun provideBackupsToolbarItems(
            selectAllToolbarMenuItem: SelectAllToolbarMenuItem,
            clearSelectionToolbarMenuItem: ClearSelectionToolbarMenuItem,
            removeToolbarMenuItem: RemoveToolbarMenuItem,
            restoreToolbarMenuItem: RestoreToolbarMenuItem,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAllToolbarMenuItem,
                clearSelectionToolbarMenuItem,
                removeToolbarMenuItem,
                restoreToolbarMenuItem
            )
        }

        /**
         * Provide rubbish bin toolbar options
         */
        @Provides
        @ElementsIntoSet
        @RubbishBin
        fun provideRubbishBinToolbarItems(
            selectAllToolbarMenuItem: SelectAllToolbarMenuItem,
            clearSelectionToolbarMenuItem: ClearSelectionToolbarMenuItem,
            removeToolbarMenuItem: RemoveToolbarMenuItem,
            restoreToolbarMenuItem: RestoreToolbarMenuItem,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAllToolbarMenuItem,
                clearSelectionToolbarMenuItem,
                removeToolbarMenuItem,
                restoreToolbarMenuItem
            )
        }
    }
}