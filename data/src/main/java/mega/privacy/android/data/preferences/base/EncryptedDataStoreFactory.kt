package mega.privacy.android.data.preferences.base

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.file
import androidx.security.crypto.streamingAead
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Creates DataStore instance stored in [EncryptedFile].
 *
 * Basic usage:
 * ```
 * val dataStore = DataStoreFactory.createEncrypted(serializer) {
 *     EncryptedFile.Builder(
 *          context.dataStoreFile("filename"),
 *          context,
 *          MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
 *          EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
 *     ).build()
 * }
 *
 * @see DataStoreFactory.create
 */
internal fun <T> DataStoreFactory.createEncrypted(
    serializer: Serializer<T>,
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
    migrations: List<DataMigration<T>> = listOf(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    produceFile: () -> EncryptedFile
): DataStore<T> {
    val encryptedFile = produceFile()

    val streamingAead = encryptedFile.streamingAead
    val file = encryptedFile.file
    val associatedData = file.name.toByteArray()

    return create(
        serializer = serializer.encrypted(streamingAead, associatedData),
        corruptionHandler = corruptionHandler,
        migrations = migrations,
        scope = scope,
        produceFile = { file },
    )
}

