package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.qualifier.LoginMutex
import javax.inject.Singleton

@Module
@DisableInstallInCheck
object InternalLoginModule {

    @LoginMutex
    @Singleton
    @Provides
    fun provideLoginMutex(): Mutex = Mutex()
}