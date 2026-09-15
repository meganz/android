package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.LegacyDatabaseHandler
import mega.privacy.android.app.SqliteDatabaseHandler
import mega.privacy.android.data.database.DatabaseHandler
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class DatabaseHandlerModule {
    @Singleton
    @Provides
    fun provideLegacyDatabaseHandler(
        databaseHandler: SqliteDatabaseHandler
    ): LegacyDatabaseHandler = databaseHandler

    @Singleton
    @Provides
    fun provideDbHandler(
        legacyDatabaseHandler: LegacyDatabaseHandler
    ): DatabaseHandler = legacyDatabaseHandler
}

/**
 * This interface is needed to inject DatabaseHandler by Hilt
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DatabaseHandlerEntryPoint {
    val dbH: LegacyDatabaseHandler
}