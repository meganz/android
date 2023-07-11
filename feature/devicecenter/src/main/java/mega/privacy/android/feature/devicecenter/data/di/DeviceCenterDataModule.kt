package mega.privacy.android.feature.devicecenter.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.feature.devicecenter.data.repository.DeviceCenterRepositoryImpl
import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository

/**
 * Class that provides implementations for Device Center Repositories
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface DeviceCenterDataModule {

    @Binds
    fun bindDeviceCenterRepository(implementation: DeviceCenterRepositoryImpl): DeviceCenterRepository
}