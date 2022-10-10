package mega.privacy.android.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors.fromApplication
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.SqliteDatabaseHandler
import mega.privacy.android.data.mapper.StorageStateIntMapper
import mega.privacy.android.data.mapper.StorageStateMapper
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class DatabaseHandlerModule {
    @Singleton
    @Provides
    fun provideDbHandler(
        @ApplicationContext context: Context,
        storageStateMapper: StorageStateMapper,
        storageStateIntMapper: StorageStateIntMapper,
    ): DatabaseHandler {
        return SqliteDatabaseHandler.getDbHandler(
            context = context,
            storageStateMapper = storageStateMapper,
            storageStateIntMapper = storageStateIntMapper
        )
    }
}

/**
 * This method is to inject DatabaseHandler into non-Android classes by Hilt
 */
fun getDbHandler(): DatabaseHandler = fromApplication(
    MegaApplication.getInstance(),
    DatabaseHandlerEntryPoint::class.java).dbH

/**
 * This interface is needed to inject DatabaseHandler by Hilt
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DatabaseHandlerEntryPoint {
    var dbH: DatabaseHandler
}