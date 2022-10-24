package mega.privacy.android.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.data.preferences.CameraTimestampsPreferenceDataStore.Companion.LAST_CAM_SYNC_TIMESTAMP_FILE
import mega.privacy.android.data.qualifier.CameraTimestampsPreference
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Singleton

/**
 * DataStore Module of DataStore<Preferences>
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {

    /**
     * provides DataStore<Preferences> for [LAST_CAM_SYNC_TIMESTAMP_FILE]
     */
    @Singleton
    @Provides
    @CameraTimestampsPreference
    fun provideCameraPreferencesDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            migrations = listOf(SharedPreferencesMigration(context,
                LAST_CAM_SYNC_TIMESTAMP_FILE)),
            scope = CoroutineScope(ioDispatcher),
            produceFile = { context.preferencesDataStoreFile(LAST_CAM_SYNC_TIMESTAMP_FILE) }
        )
    }
}
