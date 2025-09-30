package mega.privacy.android.core.nodecomponents.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.CopySelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.DownloadSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.HideSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.ManageLinkSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.MoveSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.DeletePermanentlySelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.RestoreSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.RubbishBinSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.UnhideSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.RenameSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.GetLinkSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.RemoveLinkSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.SendToChatSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.ShareFolderSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.RemoveShareSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.DisputeTakeDownSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.ShareSelectionMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.qualifier.features.CloudDrive
import mega.privacy.android.domain.qualifier.features.RubbishBin
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NodeSelectionModeModule {

    companion object {
        @Provides
        @ElementsIntoSet
        @CloudDrive
        @Singleton
        fun provideCloudDriveToolbarItems(
            copySelectionModeMenuAction: CopySelectionMenuItem,
            hideSelectionModeMenuAction: HideSelectionMenuItem,
            moveSelectionModeMenuAction: MoveSelectionMenuItem,
            rubbishBinSelectionMenuAction: RubbishBinSelectionMenuItem,
            manageLinkSelectionMenuAction: ManageLinkSelectionMenuItem,
            downloadSelectionMenuItem: DownloadSelectionMenuItem,
            unhideSelectionMenuItem: UnhideSelectionMenuItem,
            renameSelectionMenuItem: RenameSelectionMenuItem,
            getLinkSelectionMenuItem: GetLinkSelectionMenuItem,
            removeLinkSelectionMenuItem: RemoveLinkSelectionMenuItem,
            sendToChatSelectionMenuItem: SendToChatSelectionMenuItem,
            shareFolderSelectionMenuItem: ShareFolderSelectionMenuItem,
            removeShareSelectionMenuItem: RemoveShareSelectionMenuItem,
            disputeTakeDownSelectionMenuItem: DisputeTakeDownSelectionMenuItem,
            shareSelectionMenuItem: ShareSelectionMenuItem,
        ): Set<NodeSelectionMenuItem<MenuActionWithIcon>> = setOf(
            copySelectionModeMenuAction,
            hideSelectionModeMenuAction,
            moveSelectionModeMenuAction,
            rubbishBinSelectionMenuAction,
            manageLinkSelectionMenuAction,
            downloadSelectionMenuItem,
            unhideSelectionMenuItem,
            renameSelectionMenuItem,
            getLinkSelectionMenuItem,
            removeLinkSelectionMenuItem,
            sendToChatSelectionMenuItem,
            shareFolderSelectionMenuItem,
            removeShareSelectionMenuItem,
            disputeTakeDownSelectionMenuItem,
            shareSelectionMenuItem
        )

        @Provides
        @ElementsIntoSet
        @RubbishBin
        @Singleton
        fun provideRubbishBinToolbarItems(
            deletePermanentlySelectionMenuItem: DeletePermanentlySelectionMenuItem,
            restoreSelectionMenuItem: RestoreSelectionMenuItem,
        ): Set<NodeSelectionMenuItem<MenuActionWithIcon>> = setOf(
            deletePermanentlySelectionMenuItem,
            restoreSelectionMenuItem,
        )
    }
}