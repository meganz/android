package mega.privacy.android.core.nodecomponents.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.core.nodecomponents.action.clickhandler.AvailableOfflineActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.CopyActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.DeletePermanentActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.DisputeTakeDownActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.DownloadActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.EditActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.FavouriteActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.GetLinkActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.HideActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.InfoActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.LabelActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.LeaveShareActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.ManageLinkActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.ManageShareFolderActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.MoveActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.MoveToRubbishBinActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.MultiNodeAction
import mega.privacy.android.core.nodecomponents.action.clickhandler.OpenWithActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.RemoveFavouriteActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.RemoveLinkActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.RemoveOfflineActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.RemoveShareActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.RenameNodeActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.RestoreActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.SendToChatActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.ShareActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.ShareFolderActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.SingleNodeAction
import mega.privacy.android.core.nodecomponents.action.clickhandler.SyncActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.UnhideActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.VerifyActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.VersionsActionClickHandler
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
        versionsAction: VersionsActionClickHandler,
        moveAction: MoveActionClickHandler,
        copyAction: CopyActionClickHandler,
        shareFolderAction: ShareFolderActionClickHandler,
        restoreAction: RestoreActionClickHandler,
        sendToChatAction: SendToChatActionClickHandler,
        openWithAction: OpenWithActionClickHandler,
        downloadAction: DownloadActionClickHandler,
        availableOfflineAction: AvailableOfflineActionClickHandler,
        removeOfflineAction: RemoveOfflineActionClickHandler,
        hideAction: HideActionClickHandler,
        renameNodeAction: RenameNodeActionClickHandler,
        moveToRubbishBinAction: MoveToRubbishBinActionClickHandler,
        manageLinkAction: ManageLinkActionClickHandler,
        deletePermanentAction: DeletePermanentActionClickHandler,
        leaveShareAction: LeaveShareActionClickHandler,
        labelAction: LabelActionClickHandler,
        manageShareFolderAction: ManageShareFolderActionClickHandler,
        infoAction: InfoActionClickHandler,
        editAction: EditActionClickHandler,
        disputeTakeDownAction: DisputeTakeDownActionClickHandler,
        verifyAction: VerifyActionClickHandler,
        shareAction: ShareActionClickHandler,
        removeShareAction: RemoveShareActionClickHandler,
        removeLinkAction: RemoveLinkActionClickHandler,
        getLinkAction: GetLinkActionClickHandler,
        unhideAction: UnhideActionClickHandler,
        favouriteAction: FavouriteActionClickHandler,
        removeFavouriteAction: RemoveFavouriteActionClickHandler,
        syncAction: SyncActionClickHandler
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
            removeOfflineAction,
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
            removeFavouriteAction,
            syncAction
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
        openWithAction: OpenWithActionClickHandler,
        downloadAction: DownloadActionClickHandler,
        availableOfflineAction: AvailableOfflineActionClickHandler,
        removeOfflineAction: RemoveOfflineActionClickHandler,
        shareFolderAction: ShareFolderActionClickHandler,
        leaveShareAction: LeaveShareActionClickHandler,
        copyAction: CopyActionClickHandler,
        moveAction: MoveActionClickHandler,
        sendToChatAction: SendToChatActionClickHandler,
        restoreAction: RestoreActionClickHandler,
        hideAction: HideActionClickHandler,
        moveToRubbishBinAction: MoveToRubbishBinActionClickHandler,
        manageLinkAction: ManageLinkActionClickHandler,
        deletePermanentAction: DeletePermanentActionClickHandler,
        disputeTakeDownAction: DisputeTakeDownActionClickHandler,
        getLinkAction: GetLinkActionClickHandler,
        removeLinkAction: RemoveLinkActionClickHandler,
        removeShareAction: RemoveShareActionClickHandler,
        renameNodeAction: RenameNodeActionClickHandler,
        shareAction: ShareActionClickHandler,
        unhideAction: UnhideActionClickHandler
    ): Set<MultiNodeAction> {
        return setOf(
            openWithAction,
            downloadAction,
            availableOfflineAction,
            removeOfflineAction,
            shareFolderAction,
            leaveShareAction,
            copyAction,
            moveAction,
            sendToChatAction,
            restoreAction,
            hideAction,
            moveToRubbishBinAction,
            manageLinkAction,
            deletePermanentAction,
            disputeTakeDownAction,
            getLinkAction,
            removeLinkAction,
            removeShareAction,
            renameNodeAction,
            shareAction,
            unhideAction
        )
    }
}