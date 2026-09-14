package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.data.gateway.FileLogWriter
import mega.privacy.android.data.gateway.LogConfigurationGateway
import mega.privacy.android.data.gateway.LogFlushGateway
import mega.privacy.android.data.gateway.LogWriterGateway
import mega.privacy.android.data.gateway.LogbackLogConfigurationGateway
import mega.privacy.android.data.gateway.LogbackLogFlushGateway
import mega.privacy.android.data.gateway.TimberChatLogger
import mega.privacy.android.data.gateway.TimberMegaLogger
import mega.privacy.android.data.logging.LogFileTree
import mega.privacy.android.data.repository.TimberLoggingRepository
import mega.privacy.android.domain.qualifier.IoDispatcher
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

    @Singleton
    @Binds
    abstract fun bindLogFlushGateway(implementation: LogbackLogFlushGateway): LogFlushGateway

    @Singleton
    @Binds
    abstract fun bindLogConfigurationGateway(implementation: LogbackLogConfigurationGateway): LogConfigurationGateway

    companion object {
        /**
         * SLF4J logger that SDK, Chat SDK and client app logs are all written through. Must
         * match the `<logger>` declared in `assets/logback.xml`, which routes it to the one
         * rolling file appender.
         */
        const val FILE_LOGGER_NAME = "mega.privacy.android.data.logging.LogFile"

        @Singleton
        @Provides
        fun provideFileLogWriter(): LogWriterGateway =
            FileLogWriter { LoggerFactory.getLogger(FILE_LOGGER_NAME) }

        @Singleton
        @Provides
        fun provideLogFileTree(
            createLogEntry: CreateLogEntry,
            writer: LogWriterGateway,
            @IoDispatcher dispatcher: CoroutineDispatcher,
        ): LogFileTree = LogFileTree(dispatcher, createLogEntry, writer)
    }
}
