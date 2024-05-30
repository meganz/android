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
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

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
    context: Context,
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    migrations: List<DataMigration<Preferences>> = listOf(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    masterKey: MasterKey?,
    fileName: String,
): DataStore<Preferences> {
    return try {
        masterKey ?: throw IllegalArgumentException("Failed to create MasterKey")
        val encryptedFile = EncryptedFile.Builder(
            context,
            context.preferencesDataStoreFile(fileName),
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        val delegate = DataStoreFactory.createEncrypted(
            serializer = PreferencesSerializer,
            corruptionHandler = corruptionHandler,
            migrations = migrations,
            scope = scope,
            produceFile = { encryptedFile },
        )
        PreferenceDataStore(delegate)
    } catch (e: Exception) {
        // rollback to unencrypted DataStore if failed to create EncryptedFile
        // https://console.firebase.google.com/u/1/project/megapoc-25443/crashlytics/app/android:mega.privacy.android.app/issues/8a1e160bcab8e85c2ecc3334d19a75ea
        Timber.e(e, "Failed to create EncryptedFile for $fileName")
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