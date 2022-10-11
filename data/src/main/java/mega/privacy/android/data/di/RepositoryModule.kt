package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.repository.DefaultEnvironmentRepository
import mega.privacy.android.data.repository.DefaultFeatureFlagRepository
import mega.privacy.android.data.repository.DefaultGalleryFilesRepository
import mega.privacy.android.data.repository.DefaultNetworkRepository
import mega.privacy.android.data.repository.DefaultStatisticsRepository
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.FeatureFlagRepository
import mega.privacy.android.domain.repository.GalleryFilesRepository
import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.repository.StatisticsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    abstract fun bindNetworkRepository(repository: DefaultNetworkRepository): NetworkRepository

    @Binds
    abstract fun bindDeviceRepository(implementation: DefaultEnvironmentRepository): EnvironmentRepository

    @Binds
    @Singleton
    abstract fun bindFeatureFlagRepository(repository: DefaultFeatureFlagRepository): FeatureFlagRepository

    @Binds
    abstract fun bindStatisticsRepository(repository: DefaultStatisticsRepository): StatisticsRepository

    @Binds
    abstract fun bindGalleryFilesRepository(repository: DefaultGalleryFilesRepository): GalleryFilesRepository
}