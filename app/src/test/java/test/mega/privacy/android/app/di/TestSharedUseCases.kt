package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.SharedUseCaseModule
import mega.privacy.android.app.domain.usecase.CheckAccessErrorExtended
import mega.privacy.android.domain.usecase.IsDatabaseEntryStale
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [SharedUseCaseModule::class],
    components = [SingletonComponent::class]
)
@Module(includes = [TestGetNodeModule::class])
object TestSharedUseCases {

    @Provides
    fun provideIsDatabaseEntryStale() = mock<IsDatabaseEntryStale>()

    @Provides
    fun provideCheckAccessErrorExtended() = mock<CheckAccessErrorExtended>()
}
