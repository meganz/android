package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.repository.LoggingRepository
import mega.privacy.android.app.domain.usecase.DefaultInitialiseLogging
import mega.privacy.android.app.domain.usecase.InitialiseLogging
import mega.privacy.android.app.domain.usecase.ResetSdkLogger
import mega.privacy.android.app.logging.ChatLogger
import mega.privacy.android.app.logging.SdkLogger
import mega.privacy.android.app.logging.loggers.FileLogger
import mega.privacy.android.app.logging.loggers.TimberChatLogger
import mega.privacy.android.app.logging.loggers.TimberMegaLogger
import org.slf4j.LoggerFactory
import javax.inject.Singleton

/**
 * Logging module
 *
 * Provides logging specific dependencies
 *
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {

    @Singleton
    @Binds
    abstract fun bindInitialiseLogging(useCase: DefaultInitialiseLogging): InitialiseLogging

    companion object {

        @Singleton
        @SdkLogger
        @Provides
        fun provideSdkFileLogger(): FileLogger =
            FileLogger(LoggerFactory.getLogger(TimberMegaLogger::class.java))

        @Singleton
        @ChatLogger
        @Provides
        fun provideChatFileLogger(): FileLogger =
            FileLogger(LoggerFactory.getLogger(TimberChatLogger::class.java))

        @Provides
        fun provideResetSdkLogger(loggingRepository: LoggingRepository): ResetSdkLogger =
            ResetSdkLogger(loggingRepository::resetSdkLogging)

    }
}