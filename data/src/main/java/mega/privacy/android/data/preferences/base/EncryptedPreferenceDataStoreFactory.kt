@file:Suppress("UnusedReceiverParameter")

package mega.privacy.android.data.preferences.base

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.security.crypto.EncryptedFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Creates Preferences DataStore instance stored in [EncryptedFile].
 * The file must have the extension "preferences_pb".
 *
 * Basic usage:
 * ```
 * val dataStore = PreferenceDataStoreFactory.createEncrypted {
 *     EncryptedFile.Builder(
 *          context.dataStoreFile("filename.preferences_pb"),
 *          context,
 *          MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
 *          EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
 *     ).build()
 * }
 * ```
 * @see PreferenceDataStoreFactory.create
 */
fun PreferenceDataStoreFactory.createEncrypted(
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    migrations: List<DataMigration<Preferences>> = listOf(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    produceFile: () -> EncryptedFile,
): DataStore<Preferences> {
    val delegate = DataStoreFactory.createEncrypted(
        serializer = PreferencesSerializer,
        corruptionHandler = corruptionHandler,
        migrations = migrations,
        scope = scope,
        produceFile = produceFile,
    )

    return PreferenceDataStore(delegate)
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