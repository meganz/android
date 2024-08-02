package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.qualifier.ChatLogger
import mega.privacy.android.domain.qualifier.SdkLogger
import mega.privacy.android.domain.repository.LoggingRepository
import mega.privacy.android.domain.usecase.CreateChatLogEntry
import mega.privacy.android.domain.usecase.CreateLogEntry
import mega.privacy.android.domain.usecase.CreateSdkLogEntry
import mega.privacy.android.domain.usecase.CreateTraceString
import mega.privacy.android.domain.usecase.DefaultCreateTraceString
import mega.privacy.android.domain.usecase.EnableLogAllToConsole

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
        fun provideEnableLogAllToConsole(repository: LoggingRepository): EnableLogAllToConsole =
            EnableLogAllToConsole(repository::enableLogAllToConsole)
    }
}