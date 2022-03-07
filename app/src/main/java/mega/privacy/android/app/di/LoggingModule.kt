package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.repository.TimberLoggingRepository
import mega.privacy.android.app.domain.repository.LoggingRepository
import mega.privacy.android.app.domain.usecase.DefaultInitialiseLogging
import mega.privacy.android.app.domain.usecase.DefaultResetSdkLogger
import mega.privacy.android.app.domain.usecase.InitialiseLogging
import mega.privacy.android.app.domain.usecase.ResetSdkLogger
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {

    @Singleton
    @Binds
    abstract fun bindLoggingRepository(repository: TimberLoggingRepository): LoggingRepository

    @Singleton
    @Binds
    abstract fun bindInitialiseLogging(useCase: DefaultInitialiseLogging): InitialiseLogging

    @Binds
    abstract fun bindResetSdkLogger(useCase: DefaultResetSdkLogger): ResetSdkLogger
}