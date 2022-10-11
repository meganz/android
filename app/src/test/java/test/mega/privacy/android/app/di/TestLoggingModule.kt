package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.LoggingModule
import mega.privacy.android.data.gateway.LogWriterGateway
import mega.privacy.android.data.qualifier.ChatLogger
import mega.privacy.android.data.qualifier.SdkLogger
import mega.privacy.android.domain.usecase.CreateLogEntry
import mega.privacy.android.domain.usecase.CreateTraceString
import mega.privacy.android.domain.usecase.EnableLogAllToConsole
import mega.privacy.android.domain.usecase.GetCurrentTimeString
import mega.privacy.android.domain.usecase.GetLogFile
import mega.privacy.android.domain.usecase.InitialiseLogging
import mega.privacy.android.domain.usecase.ResetSdkLogger
import nz.mega.sdk.MegaChatLoggerInterface
import nz.mega.sdk.MegaLoggerInterface
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    replaces = [LoggingModule::class],
    components = [SingletonComponent::class]
)
object TestLoggingModule {

    @Provides
    fun bindMegaChatLoggerInterface(): MegaChatLoggerInterface = mock()

    @Provides
    fun bindMegaLoggerInterface(): MegaLoggerInterface = mock()

    @Provides
    @SdkLogger
    fun bindCreateSdkLogEntry(): CreateLogEntry = mock()

    @Provides
    @ChatLogger
    fun bindCreateChatLogEntry(): CreateLogEntry = mock()

    @Provides
    fun bindCreateTraceString(): CreateTraceString = mock()

    @Provides
    fun bindGetCurrentTimeString(): GetCurrentTimeString =
        mock()

    @SdkLogger
    @Provides
    fun provideSdkFileLogger(): LogWriterGateway = mock()

    @ChatLogger
    @Provides
    fun provideChatFileLogger(): LogWriterGateway = mock()

    @Provides
    fun provideResetSdkLogger(): ResetSdkLogger = mock()

    @Provides
    fun provideInitialiseLogging(): InitialiseLogging = mock()

    @Provides
    fun provideGetLogFile(): GetLogFile = mock()

    @Provides
    fun provideEnableLogAllToConsole(): EnableLogAllToConsole = mock()
}