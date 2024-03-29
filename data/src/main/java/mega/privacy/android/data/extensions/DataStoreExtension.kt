package mega.privacy.android.data.extensions

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * read data store preferences and returns [emptyPreferences] if [IOException] is thrown
 */
fun <T> DataStore<Preferences>.monitor(key: Preferences.Key<T>): Flow<T?> {
    return this.data.monitor(key)
}

/**
 * read data store preferences and returns [emptyPreferences] if [IOException] is thrown
 */
fun <T> Flow<Preferences>.monitor(key: Preferences.Key<T>): Flow<T?> {
    return this
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map {
            it[key]
        }
}
