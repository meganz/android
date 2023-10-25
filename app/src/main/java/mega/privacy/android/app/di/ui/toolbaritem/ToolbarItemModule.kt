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
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Download
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.GetLink
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.LeaveShare
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ManageLink
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Move
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.NodeToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Remove
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveLinkDropDown
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
            download: Download,
            removeShare: RemoveShare,
            getLink: GetLink,
            manageLink: ManageLink,
            removeLinkDropDown: RemoveLinkDropDown,
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
                getLink,
                manageLink,
                removeLinkDropDown,
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
            download: Download,
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
            download: Download,
            getLink: GetLink,
            manageLink: ManageLink,
            removeLinkDropDown: RemoveLinkDropDown,
            sendToChat: SendToChat,
            rename: Rename,
            copy: Copy,
            trash: Trash,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                download,
                getLink,
                manageLink,
                removeLinkDropDown,
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
            manageLink: ManageLink,
            getLink: GetLink,
            removeLinkDropDown: RemoveLinkDropDown,
            download: Download,
            trash: Trash,
            copy: Copy,
            move: Move,
            sendToChat: SendToChat,
            shareFolder: ShareFolder,
            disputeTakeDown: DisputeTakeDown,
            share: Share,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                download,
                disputeTakeDown,
                move,
                getLink,
                manageLink,
                removeLinkDropDown,
                sendToChat,
                shareFolder,
                share,
                rename,
                copy,
                trash,
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