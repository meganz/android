package mega.privacy.android.core.nodecomponents.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.CopySelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.DownloadSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.HideSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.ManageLinkSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.MoveSelectionMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode.RubbishBinSelectionMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.qualifier.features.CloudDrive
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
        ): Set<NodeSelectionMenuItem<*>> = setOf(
            copySelectionModeMenuAction,
            hideSelectionModeMenuAction,
            moveSelectionModeMenuAction,
            rubbishBinSelectionMenuAction,
            manageLinkSelectionMenuAction,
            downloadSelectionMenuItem
        )
    }
}