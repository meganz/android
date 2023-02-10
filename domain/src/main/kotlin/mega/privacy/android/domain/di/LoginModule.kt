package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Login module.
 */
@Module(includes = [InternalLoginModule::class])
@InstallIn(SingletonComponent::class)
class LoginModule