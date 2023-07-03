package mega.privacy.android.feature.sync.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.feature.sync.data.gateway.SyncGateway
import mega.privacy.android.feature.sync.data.gateway.SyncGatewayImpl
import mega.privacy.android.feature.sync.data.repository.SyncNewFolderParamsRepositoryImpl
import mega.privacy.android.feature.sync.data.repository.SyncPreferencesRepositoryImpl
import mega.privacy.android.feature.sync.data.repository.SyncRepositoryImpl
import mega.privacy.android.feature.sync.domain.repository.SyncNewFolderParamsRepository
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SyncDataModule {

    @Binds
    @Singleton
    fun bindSyncNewFolderParamsRepository(implementation: SyncNewFolderParamsRepositoryImpl): SyncNewFolderParamsRepository

    @Binds
    @Singleton
    fun bindSyncRepository(implementation: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    fun bindSyncPreferencesRepository(implementation: SyncPreferencesRepositoryImpl): SyncPreferencesRepository

    @Binds
    @Singleton
    fun bindSyncGateway(implementation: SyncGatewayImpl): SyncGateway
}
