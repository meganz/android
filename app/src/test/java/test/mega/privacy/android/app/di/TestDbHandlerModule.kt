package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.LegacyDatabaseHandler
import mega.privacy.android.app.di.DatabaseHandlerModule
import mega.privacy.android.data.database.DatabaseHandler
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseHandlerModule::class]
)
object TestDbHandlerModule {
    @Provides
    fun provideLegacyDbHandler(): LegacyDatabaseHandler = mock()

    @Provides
    fun provideDbHandler(): DatabaseHandler = mock()
}