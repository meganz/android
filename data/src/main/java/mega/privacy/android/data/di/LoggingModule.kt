package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.data.gateway.FileLogWriter
import mega.privacy.android.data.gateway.LogWriterGateway
import mega.privacy.android.data.gateway.TimberChatLogger
import mega.privacy.android.data.gateway.TimberMegaLogger
import mega.privacy.android.data.logging.LogFlowTree
import mega.privacy.android.data.repository.TimberLoggingRepository
import mega.privacy.android.domain.qualifier.ChatLogger
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.SdkLogger
import mega.privacy.android.domain.repository.LoggingRepository
import mega.privacy.android.domain.usecase.CreateLogEntry
import nz.mega.sdk.MegaChatLoggerInterface
import nz.mega.sdk.MegaLoggerInterface
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LoggingModule {
    @Singleton
    @Binds
    abstract fun bindLoggingRepository(repository: TimberLoggingRepository): LoggingRepository

    @Singleton
    @Binds
    abstract fun bindMegaChatLoggerInterface(implementation: TimberChatLogger): MegaChatLoggerInterface

    @Singleton
    @Binds
    abstract fun bindMegaLoggerInterface(implementation: TimberMegaLogger): MegaLoggerInterface

    companion object {
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
    }
}