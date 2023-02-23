package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Login module.
 */
@Module(includes = [InternalSharesModule::class])
@InstallIn(SingletonComponent::class)
class SharesModule