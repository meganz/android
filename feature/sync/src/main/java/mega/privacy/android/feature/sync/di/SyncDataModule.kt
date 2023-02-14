package mega.privacy.android.feature.sync.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.feature.sync.data.repository.SyncRepositoryImpl
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SyncDataModule {

    @Binds
    @Singleton
    fun bindSyncRepository(implementation: SyncRepositoryImpl): SyncRepository
}