package mega.privacy.android.data.preferences.base

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.google.crypto.tink.StreamingAead
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import mega.privacy.android.data.cryptography.FileStreamingAeadFactory
import java.io.File

/**
 * Creates DataStore instance whose file content is encrypted with [streamingAead], using the
 * file name as associated data.
 *
 * @see DataStoreFactory.create
 */
internal fun <T> DataStoreFactory.createEncrypted(
    serializer: Serializer<T>,
    streamingAead: StreamingAead,
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
    migrations: List<DataMigration<T>> = listOf(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    produceFile: () -> File,
): DataStore<T> {
    val file = produceFile()
    val associatedData = FileStreamingAeadFactory.associatedData(file.name)

    return create(
        serializer = serializer.encrypted(streamingAead, associatedData),
        corruptionHandler = corruptionHandler,
        migrations = migrations,
        scope = scope,
        produceFile = { file },
    )
}
