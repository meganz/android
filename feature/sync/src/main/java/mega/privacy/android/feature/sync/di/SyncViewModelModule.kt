package mega.privacy.android.feature.sync.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.GetFolderPairs
import mega.privacy.android.feature.sync.domain.usecase.GetRemoteFolders
import mega.privacy.android.feature.sync.domain.usecase.DefaultGetRemoteFolders
import mega.privacy.android.feature.sync.domain.usecase.ObserveSyncState
import mega.privacy.android.feature.sync.domain.usecase.RemoveFolderPairs
import mega.privacy.android.feature.sync.domain.usecase.SyncFolderPair

/**
 * Dagger module for Sync feature
 */
@Module
@InstallIn(ViewModelComponent::class)
internal interface SyncViewModelModule {

    companion object {

        @Provides
        fun provideGetFolderPairs(syncRepository: SyncRepository): GetFolderPairs =
            GetFolderPairs(syncRepository::getFolderPairs)

        @Provides
        fun provideRemoveFolderPairs(syncRepository: SyncRepository): RemoveFolderPairs =
            RemoveFolderPairs(syncRepository::removeFolderPairs)

        @Provides
        fun provideObserveSyncState(syncRepository: SyncRepository): ObserveSyncState =
            ObserveSyncState(syncRepository::observeSyncState)

        @Provides
        fun provideSyncFolderPair(syncRepository: SyncRepository): SyncFolderPair =
            SyncFolderPair { localPath, remotePath ->
                syncRepository.setupFolderPair(localPath, remotePath.id)
            }
    }

    @Binds
    fun bindGetRemoteFolders(impl: DefaultGetRemoteFolders): GetRemoteFolders
}
