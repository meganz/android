package mega.privacy.android.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.preferences.CameraUploadPreferencesGateway
import mega.privacy.android.app.di.IoDispatcher
import java.io.IOException
import javax.inject.Inject

private val Context.cameraUploadDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "CAMERA_UPLOAD_PREFERENCES"
)

/**
 * Camera Upload preferences data store implementation of the [CameraUploadPreferencesGateway]
 *
 * @property context
 * @property ioDispatcher
 * @constructor Create empty camera upload preferences data store
 **/
class CameraUploadPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CameraUploadPreferencesGateway {

    private val isCameraUploadRunningKey = booleanPreferencesKey("IS_CAMERA_UPLOAD_RUNNING")

    override fun isCameraUploadRunning(): Flow<Boolean> =
        getPreference(isCameraUploadRunningKey, false)

    override suspend fun setIsCameraUploadRunning(isRunning: Boolean) =
        setPreference(isCameraUploadRunningKey, isRunning)

    private suspend fun <T> setPreference(
        key: Preferences.Key<T>,
        value: T,
    ) {
        withContext(ioDispatcher) {
            context.cameraUploadDataStore.edit {
                it[key] = value
            }
        }
    }

    private fun <T> getPreference(
        key: Preferences.Key<T>,
        defaultValue: T,
    ): Flow<T> =
        context.cameraUploadDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map {
                it[key] ?: defaultValue
            }

    override suspend fun clearPreferences() {
        withContext(ioDispatcher) {
            context.cameraUploadDataStore.edit {
                it.clear()
            }
        }
    }
}
