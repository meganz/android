package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.domain.qualifier.ChatLogger
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.SdkLogger
import mega.privacy.android.domain.repository.LoggingRepository
import mega.privacy.android.domain.usecase.AreChatLogsEnabled
import mega.privacy.android.domain.usecase.AreSdkLogsEnabled
import mega.privacy.android.domain.usecase.CreateChatLogEntry
import mega.privacy.android.domain.usecase.CreateLogEntry
import mega.privacy.android.domain.usecase.CreateSdkLogEntry
import mega.privacy.android.domain.usecase.CreateTraceString
import mega.privacy.android.domain.usecase.DefaultCreateTraceString
import mega.privacy.android.domain.usecase.DefaultInitialiseLogging
import mega.privacy.android.domain.usecase.EnableLogAllToConsole
import mega.privacy.android.domain.usecase.GetLogFile
import mega.privacy.android.domain.usecase.InitialiseLogging
import mega.privacy.android.domain.usecase.ResetSdkLogger

/**
 * Logging module
 *
 * Provides logging specific dependencies
 *
 */
@Module
@DisableInstallInCheck
internal abstract class InternalLoggingModule {

    @Binds
    @SdkLogger
    abstract fun bindCreateSdkLogEntry(implementation: CreateSdkLogEntry): CreateLogEntry

    @Binds
    @ChatLogger
    abstract fun bindCreateChatLogEntry(implementation: CreateChatLogEntry): CreateLogEntry

    @Binds
    abstract fun bindCreateTraceString(implementation: DefaultCreateTraceString): CreateTraceString

    companion object {
        @Provides
        fun provideResetSdkLogger(loggingRepository: LoggingRepository): ResetSdkLogger =
            ResetSdkLogger(loggingRepository::resetSdkLogging)

        @Provides
        fun provideInitialiseLogging(
            loggingRepository: LoggingRepository,
            areSdkLogsEnabled: AreSdkLogsEnabled,
            areChatLogsEnabled: AreChatLogsEnabled,
            @IoDispatcher coroutineDispatcher: CoroutineDispatcher,
        ): InitialiseLogging = DefaultInitialiseLogging(
            loggingRepository = loggingRepository,
            areSdkLogsEnabled = areSdkLogsEnabled,
            areChatLogsEnabled = areChatLogsEnabled,
            coroutineDispatcher = coroutineDispatcher,
        )

        @Provides
        fun provideGetLogFile(repository: LoggingRepository): GetLogFile =
            GetLogFile(repository::compressLogs)

        @Provides
        fun provideEnableLogAllToConsole(repository: LoggingRepository): EnableLogAllToConsole =
            EnableLogAllToConsole(repository::enableLogAllToConsole)
    }
}