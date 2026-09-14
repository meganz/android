package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.domain.di.LoggingModule as DomainLoggingModule
import mega.privacy.android.domain.usecase.CreateLogEntry
import mega.privacy.android.domain.usecase.CreateTraceString
import mega.privacy.android.domain.usecase.EnableLogAllToConsole
import mega.privacy.android.domain.usecase.InitialiseLoggingUseCase
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    replaces = [DomainLoggingModule::class],
    components = [SingletonComponent::class]
)
object TestLoggingModule {

    @Provides
    fun bindCreateLogEntry(): CreateLogEntry = mock()

    @Provides
    fun bindCreateTraceString(): CreateTraceString = mock()

    @Provides
    fun provideInitialiseLogging(): InitialiseLoggingUseCase = mock()

    @Provides
    fun provideEnableLogAllToConsole(): EnableLogAllToConsole = mock()
}