@file:Suppress("UnusedReceiverParameter")

package mega.privacy.android.data.preferences.base

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.crypto.tink.StreamingAead
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Creates Preferences DataStore instance whose file is encrypted with [streamingAead].
 * The file must have the extension "preferences_pb".
 *
 * Falls back to an unencrypted DataStore when [streamingAead] is null or the encrypted
 * DataStore cannot be created.
 *
 * @see PreferenceDataStoreFactory.create
 */
fun PreferenceDataStoreFactory.createEncrypted(
    context: Context,
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    migrations: List<DataMigration<Preferences>> = listOf(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    streamingAead: StreamingAead?,
    fileName: String,
): DataStore<Preferences> {
    return try {
        streamingAead ?: throw IllegalArgumentException("Failed to create StreamingAead")
        val delegate = DataStoreFactory.createEncrypted(
            serializer = PreferencesSerializer,
            streamingAead = streamingAead,
            corruptionHandler = corruptionHandler,
            migrations = migrations,
            scope = scope,
            produceFile = { context.preferencesDataStoreFile(fileName) },
        )
        PreferenceDataStore(delegate)
    } catch (e: Exception) {
        // https://console.firebase.google.com/u/1/project/megapoc-25443/crashlytics/app/android:mega.privacy.android.app/issues/8a1e160bcab8e85c2ecc3334d19a75ea
        Timber.e(e, "Failed to create encrypted DataStore for $fileName")
        create(
            corruptionHandler = corruptionHandler,
            scope = scope,
            produceFile = { context.preferencesDataStoreFile(fileName) }
        )
    }
}

internal class PreferenceDataStore(private val delegate: DataStore<Preferences>) :
    DataStore<Preferences> by delegate {
    private val mutex = Mutex()
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences):
            Preferences {
        return mutex.withLock {
            delegate.updateData {
                val transformed = transform(it)
                transformed
            }
        }
    }
}
