package mega.privacy.android.feature.sync.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.feature.sync.folderpicker.SyncFolderPickerHandlerImpl
import mega.privacy.android.shared.sync.folderpicker.SyncFolderPickerHandler

@Module
@InstallIn(SingletonComponent::class)
internal interface SyncFolderPickerModule {

    /**
     * Binds the [SyncFolderPickerHandler] used by the cloud explorer to apply the sync specific
     * logic of the MEGA folder picker
     */
    @Binds
    fun bindSyncFolderPickerHandler(impl: SyncFolderPickerHandlerImpl): SyncFolderPickerHandler
}
