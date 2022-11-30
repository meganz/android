package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Account module
 *
 */
@Module(includes = [InternalSharedUseCaseModule::class])
@InstallIn(SingletonComponent::class)
abstract class SharedUseCaseModule