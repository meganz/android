package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module(includes = [InternalLogoutModule::class])
@InstallIn(SingletonComponent::class)
abstract class LogoutModule