package mega.privacy.android.core.nodecomponents.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.core.nodecomponents.action.AvailableOfflineAction
import mega.privacy.android.core.nodecomponents.action.CopyAction
import mega.privacy.android.core.nodecomponents.action.DeletePermanentAction
import mega.privacy.android.core.nodecomponents.action.DisputeTakeDownAction
import mega.privacy.android.core.nodecomponents.action.DownloadAction
import mega.privacy.android.core.nodecomponents.action.EditAction
import mega.privacy.android.core.nodecomponents.action.FavouriteAction
import mega.privacy.android.core.nodecomponents.action.GetLinkAction
import mega.privacy.android.core.nodecomponents.action.HideAction
import mega.privacy.android.core.nodecomponents.action.InfoAction
import mega.privacy.android.core.nodecomponents.action.LabelAction
import mega.privacy.android.core.nodecomponents.action.LeaveShareAction
import mega.privacy.android.core.nodecomponents.action.ManageLinkAction
import mega.privacy.android.core.nodecomponents.action.ManageShareFolderAction
import mega.privacy.android.core.nodecomponents.action.MoveAction
import mega.privacy.android.core.nodecomponents.action.MoveToRubbishBinAction
import mega.privacy.android.core.nodecomponents.action.MultiNodeAction
import mega.privacy.android.core.nodecomponents.action.OpenWithAction
import mega.privacy.android.core.nodecomponents.action.RemoveFavouriteAction
import mega.privacy.android.core.nodecomponents.action.RemoveLinkAction
import mega.privacy.android.core.nodecomponents.action.RemoveShareAction
import mega.privacy.android.core.nodecomponents.action.RenameNodeAction
import mega.privacy.android.core.nodecomponents.action.RestoreAction
import mega.privacy.android.core.nodecomponents.action.SendToChatAction
import mega.privacy.android.core.nodecomponents.action.ShareAction
import mega.privacy.android.core.nodecomponents.action.ShareFolderAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeAction
import mega.privacy.android.core.nodecomponents.action.UnhideAction
import mega.privacy.android.core.nodecomponents.action.VerifyAction
import mega.privacy.android.core.nodecomponents.action.VersionsAction
import javax.inject.Singleton

/**
 * Hilt module for providing NodeActionHandler dependencies using ElementsIntoSet.
 * This module provides sets of action handlers for single and multiple nodes operations.
 */
@Module
@InstallIn(SingletonComponent::class)
object NodeActionHandlerModule {
    /**
     * Provides all single node action handlers as a set.
     * This allows for easy iteration and validation of available handlers.
     * @see NodeActionType.kt
     */
    @Provides
    @ElementsIntoSet
    @Singleton
    fun provideSingleNodeActionHandlers(
        versionsAction: VersionsAction,
        moveAction: MoveAction,
        copyAction: CopyAction,
        shareFolderAction: ShareFolderAction,
        restoreAction: RestoreAction,
        sendToChatAction: SendToChatAction,
        openWithAction: OpenWithAction,
        downloadAction: DownloadAction,
        availableOfflineAction: AvailableOfflineAction,
        hideAction: HideAction,
        renameNodeAction: RenameNodeAction,
        moveToRubbishBinAction: MoveToRubbishBinAction,
        manageLinkAction: ManageLinkAction,
        deletePermanentAction: DeletePermanentAction,
        leaveShareAction: LeaveShareAction,
        labelAction: LabelAction,
        manageShareFolderAction: ManageShareFolderAction,
        infoAction: InfoAction,
        editAction: EditAction,
        disputeTakeDownAction: DisputeTakeDownAction,
        verifyAction: VerifyAction,
        shareAction: ShareAction,
        removeShareAction: RemoveShareAction,
        removeLinkAction: RemoveLinkAction,
        getLinkAction: GetLinkAction,
        unhideAction: UnhideAction,
        favouriteAction: FavouriteAction,
        removeFavouriteAction: RemoveFavouriteAction
    ): Set<SingleNodeAction> {
        return setOf(
            versionsAction,
            moveAction,
            copyAction,
            shareFolderAction,
            restoreAction,
            sendToChatAction,
            openWithAction,
            downloadAction,
            availableOfflineAction,
            hideAction,
            renameNodeAction,
            moveToRubbishBinAction,
            manageLinkAction,
            deletePermanentAction,
            leaveShareAction,
            labelAction,
            manageShareFolderAction,
            infoAction,
            editAction,
            disputeTakeDownAction,
            verifyAction,
            shareAction,
            removeShareAction,
            removeLinkAction,
            getLinkAction,
            unhideAction,
            favouriteAction,
            removeFavouriteAction
        )
    }

    /**
     * Provides all multiple nodes action handlers as a set.
     * This allows for easy iteration and validation of available handlers.
     * @see NodeActionType.kt
     */
    @Provides
    @ElementsIntoSet
    @Singleton
    fun provideMultipleNodesActionHandlers(
        openWithAction: OpenWithAction,
        downloadAction: DownloadAction,
        availableOfflineAction: AvailableOfflineAction,
        shareFolderAction: ShareFolderAction,
        copyAction: CopyAction,
        moveAction: MoveAction,
        sendToChatAction: SendToChatAction,
        restoreAction: RestoreAction,
        hideAction: HideAction,
        moveToRubbishBinAction: MoveToRubbishBinAction,
        manageLinkAction: ManageLinkAction,
        deletePermanentAction: DeletePermanentAction,
    ): Set<MultiNodeAction> {
        return setOf(
            openWithAction,
            downloadAction,
            availableOfflineAction,
            shareFolderAction,
            copyAction,
            moveAction,
            sendToChatAction,
            restoreAction,
            hideAction,
            moveToRubbishBinAction,
            manageLinkAction,
            deletePermanentAction
        )
    }
}