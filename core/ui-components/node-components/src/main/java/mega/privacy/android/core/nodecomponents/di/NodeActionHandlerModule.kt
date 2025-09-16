package mega.privacy.android.core.nodecomponents.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.core.nodecomponents.action.AvailableOfflineAction
import mega.privacy.android.core.nodecomponents.action.ClearSelectionAction
import mega.privacy.android.core.nodecomponents.action.CopyAction
import mega.privacy.android.core.nodecomponents.action.DeletePermanentAction
import mega.privacy.android.core.nodecomponents.action.DownloadAction
import mega.privacy.android.core.nodecomponents.action.HideAction
import mega.privacy.android.core.nodecomponents.action.ManageLinkAction
import mega.privacy.android.core.nodecomponents.action.MoveAction
import mega.privacy.android.core.nodecomponents.action.MoveToRubbishBinAction
import mega.privacy.android.core.nodecomponents.action.MultiNodeAction
import mega.privacy.android.core.nodecomponents.action.OpenWithAction
import mega.privacy.android.core.nodecomponents.action.RenameNodeAction
import mega.privacy.android.core.nodecomponents.action.RestoreAction
import mega.privacy.android.core.nodecomponents.action.SelectAllAction
import mega.privacy.android.core.nodecomponents.action.SendToChatAction
import mega.privacy.android.core.nodecomponents.action.ShareFolderAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeAction
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
            deletePermanentAction
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
        selectAllAction: SelectAllAction,
        clearSelectionAction: ClearSelectionAction,
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
            selectAllAction,
            clearSelectionAction,
            restoreAction,
            hideAction,
            moveToRubbishBinAction,
            manageLinkAction,
            deletePermanentAction
        )
    }
}