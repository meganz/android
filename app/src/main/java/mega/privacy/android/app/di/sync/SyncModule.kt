package mega.privacy.android.app.di.sync

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.sync.GetRemoteFolders
import mega.privacy.android.app.domain.usecase.sync.GetRemoteFoldersImpl
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.repository.SyncRepository
import mega.privacy.android.domain.usecase.sync.GetSyncLocalPath
import mega.privacy.android.domain.usecase.sync.GetSyncRemotePath
import mega.privacy.android.domain.usecase.sync.SetSyncLocalPath
import mega.privacy.android.domain.usecase.sync.SetSyncRemotePath

/**
 * Dagger module for Sync feature
 */
@Module
@InstallIn(ViewModelComponent::class)
class SyncModule {

    /**
     * provides SetSyncLocalPath use case
     */
    @Provides
    fun provideSetSyncLocalPath(syncRepository: SyncRepository): SetSyncLocalPath =
        SetSyncLocalPath(syncRepository::setSyncLocalPath)

    /**
     * provides GetSyncLocalPath use case
     */
    @Provides
    fun provideGetSyncLocalPath(syncRepository: SyncRepository): GetSyncLocalPath =
        GetSyncLocalPath(syncRepository::getSyncLocalPath)

    /**
     * provides GetSyncRemotePath use case
     */
    @Provides
    fun provideGetSyncRemotePath(syncRepository: SyncRepository): GetSyncRemotePath =
        GetSyncRemotePath(syncRepository::getSyncRemotePath)

    /**
     * provides SetSyncRemotePath use case
     */
    @Provides
    fun provideSetSyncRemotePath(syncRepository: SyncRepository): SetSyncRemotePath =
        SetSyncRemotePath(syncRepository::setSyncRemotePath)

    /**
     * provides GetRemoteFolders use case
     */
    @Provides
    fun provideGetRemoteFolders(megaNodeRepository: MegaNodeRepository): GetRemoteFolders =
        GetRemoteFoldersImpl(megaNodeRepository)
}
