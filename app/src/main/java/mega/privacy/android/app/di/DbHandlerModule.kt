package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors.fromApplication
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.LegacyDatabaseHandler
import mega.privacy.android.app.MegaApplication
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
 * This method is to inject DatabaseHandler into non-Android classes by Hilt
 */
fun getDbHandler(): LegacyDatabaseHandler = fromApplication(
    MegaApplication.getInstance(),
    DatabaseHandlerEntryPoint::class.java).dbH

/**
 * This interface is needed to inject DatabaseHandler by Hilt
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DatabaseHandlerEntryPoint {
    var dbH: LegacyDatabaseHandler
}