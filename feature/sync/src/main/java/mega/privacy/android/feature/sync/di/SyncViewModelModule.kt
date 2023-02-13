package mega.privacy.android.feature.sync.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.GetRemoteFolders
import mega.privacy.android.feature.sync.domain.usecase.GetRemoteFoldersImpl
import mega.privacy.android.feature.sync.domain.usecase.GetSyncLocalPath
import mega.privacy.android.feature.sync.domain.usecase.GetSyncRemotePath
import mega.privacy.android.feature.sync.domain.usecase.SetSyncLocalPath
import mega.privacy.android.feature.sync.domain.usecase.SetSyncRemotePath

/**
 * Dagger module for Sync feature
 */
@Module
@InstallIn(ViewModelComponent::class)
internal class SyncViewModelModule {

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
