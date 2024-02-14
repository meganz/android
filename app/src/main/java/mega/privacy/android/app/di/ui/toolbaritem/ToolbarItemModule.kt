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
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ClearSelection
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Copy
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.DisputeTakeDown
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.DownloadToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.GetLinkToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.LeaveShare
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ManageLink
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Move
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.MultiSelectManageLink
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.NodeToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Remove
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveLinkToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveShare
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Rename
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Restore
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.SelectAll
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.SendToChat
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Share
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ShareFolder
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Trash

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
            selectAll: SelectAll,
            clearSelection: ClearSelection,
            download: DownloadToolbarMenuItem,
            removeShare: RemoveShare,
            getLinkToolbarMenuItem: GetLinkToolbarMenuItem,
            manageLink: ManageLink,
            removeLinkToolbarMenuItem: RemoveLinkToolbarMenuItem,
            sendToChat: SendToChat,
            shareFolder: ShareFolder,
            share: Share,
            rename: Rename,
            copy: Copy,
            trash: Trash,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                download,
                removeShare,
                getLinkToolbarMenuItem,
                manageLink,
                removeLinkToolbarMenuItem,
                sendToChat,
                shareFolder,
                share,
                rename,
                copy,
                trash,
            )
        }

        /**
         * Provide incoming shares toolbar options
         */
        @Provides
        @ElementsIntoSet
        @IncomingShares
        fun provideIncomingSharesToolbarItems(
            selectAll: SelectAll,
            clearSelection: ClearSelection,
            leaveShare: LeaveShare,
            download: DownloadToolbarMenuItem,
            move: Move,
            copy: Copy,
            sendToChat: SendToChat,
            rename: Rename,
            trash: Trash,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                download,
                leaveShare,
                move,
                sendToChat,
                rename,
                copy,
                trash,
            )
        }

        /**
         * Provide shared links toolbar options
         */
        @Provides
        @ElementsIntoSet
        @Links
        fun provideSharedLinksToolbarItems(
            selectAll: SelectAll,
            clearSelection: ClearSelection,
            download: DownloadToolbarMenuItem,
            getLinkToolbarMenuItem: GetLinkToolbarMenuItem,
            manageLink: ManageLink,
            removeLinkToolbarMenuItem: RemoveLinkToolbarMenuItem,
            sendToChat: SendToChat,
            rename: Rename,
            copy: Copy,
            trash: Trash,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                download,
                getLinkToolbarMenuItem,
                manageLink,
                removeLinkToolbarMenuItem,
                sendToChat,
                rename,
                copy,
                trash,
            )
        }

        /**
         * Provide cloud drive toolbar options
         */
        @Provides
        @ElementsIntoSet
        @CloudDrive
        fun provideCloudDriveToolbarItems(
            selectAll: SelectAll,
            clearSelection: ClearSelection,
            rename: Rename,
            manageLink: MultiSelectManageLink,
            getLinkToolbarMenuItem: GetLinkToolbarMenuItem,
            removeLinkToolbarMenuItem: RemoveLinkToolbarMenuItem,
            download: DownloadToolbarMenuItem,
            trash: Trash,
            copy: Copy,
            move: Move,
            sendToChat: SendToChat,
            shareFolder: ShareFolder,
            removeShare: RemoveShare,
            disputeTakeDown: DisputeTakeDown,
            share: Share,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                download,
                disputeTakeDown,
                move,
                getLinkToolbarMenuItem,
                manageLink,
                removeLinkToolbarMenuItem,
                sendToChat,
                shareFolder,
                share,
                rename,
                copy,
                trash,
                removeShare,
            )
        }

        /**
         * Provide backups toolbar options
         */
        @Provides
        @ElementsIntoSet
        @Backups
        fun provideBackupsToolbarItems(
            selectAll: SelectAll,
            clearSelection: ClearSelection,
            remove: Remove,
            restore: Restore,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                remove,
                restore
            )
        }

        /**
         * Provide rubbish bin toolbar options
         */
        @Provides
        @ElementsIntoSet
        @RubbishBin
        fun provideRubbishBinToolbarItems(
            selectAll: SelectAll,
            clearSelection: ClearSelection,
            remove: Remove,
            restore: Restore,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                remove,
                restore
            )
        }
    }
}