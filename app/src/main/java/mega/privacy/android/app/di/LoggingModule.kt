package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.app.data.gateway.FileLogWriter
import mega.privacy.android.app.data.gateway.LogWriterGateway
import mega.privacy.android.app.data.gateway.TimberChatLogger
import mega.privacy.android.app.data.gateway.TimberMegaLogger
import mega.privacy.android.app.domain.usecase.GetCurrentTimeStringFromCalendar
import mega.privacy.android.app.logging.ChatLogger
import mega.privacy.android.app.logging.SdkLogger
import mega.privacy.android.app.presentation.logging.tree.LogFlowTree
import mega.privacy.android.domain.repository.LoggingRepository
import mega.privacy.android.domain.usecase.AreChatLogsEnabled
import mega.privacy.android.domain.usecase.AreSdkLogsEnabled
import mega.privacy.android.domain.usecase.CreateChatLogEntry
import mega.privacy.android.domain.usecase.CreateLogEntry
import mega.privacy.android.domain.usecase.CreateSdkLogEntry
import mega.privacy.android.domain.usecase.CreateTraceString
import mega.privacy.android.domain.usecase.DefaultCreateTraceString
import mega.privacy.android.domain.usecase.DefaultInitialiseLogging
import mega.privacy.android.domain.usecase.GetCurrentTimeString
import mega.privacy.android.domain.usecase.InitialiseLogging
import mega.privacy.android.domain.usecase.ResetSdkLogger
import nz.mega.sdk.MegaChatLoggerInterface
import nz.mega.sdk.MegaLoggerInterface
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

    @Binds
    abstract fun bindMegaChatLoggerInterface(implementation: TimberChatLogger): MegaChatLoggerInterface

    @Binds
    abstract fun bindMegaLoggerInterface(implementation: TimberMegaLogger): MegaLoggerInterface

    @Binds
    @SdkLogger
    abstract fun bindCreateSdkLogEntry(implementation: CreateSdkLogEntry): CreateLogEntry

    @Binds
    @ChatLogger
    abstract fun bindCreateChatLogEntry(implementation: CreateChatLogEntry): CreateLogEntry

    @Binds
    abstract fun bindCreateTraceString(implementation: DefaultCreateTraceString): CreateTraceString

    @Binds
    abstract fun bindGetCurrentTimeString(implementation: GetCurrentTimeStringFromCalendar): GetCurrentTimeString


    companion object {

        @Singleton
        @SdkLogger
        @Provides
        fun provideSdkFileLogger(): LogWriterGateway =
            FileLogWriter(LoggerFactory.getLogger(TimberMegaLogger::class.java))

        @Singleton
        @ChatLogger
        @Provides
        fun provideChatFileLogger(): LogWriterGateway =
            FileLogWriter(LoggerFactory.getLogger(TimberChatLogger::class.java))

        @Provides
        fun provideResetSdkLogger(loggingRepository: LoggingRepository): ResetSdkLogger =
            ResetSdkLogger(loggingRepository::resetSdkLogging)

        @SdkLogger
        @Provides
        fun provideSdkLogFlowTree(
            @SdkLogger useCase: CreateLogEntry,
            @IoDispatcher dispatcher: CoroutineDispatcher,
        ): LogFlowTree = LogFlowTree(dispatcher, useCase)

        @ChatLogger
        @Provides
        fun provideChatLogFlowTree(
            @ChatLogger useCase: CreateLogEntry,
            @IoDispatcher dispatcher: CoroutineDispatcher,
        ): LogFlowTree = LogFlowTree(dispatcher, useCase)

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
    }
}