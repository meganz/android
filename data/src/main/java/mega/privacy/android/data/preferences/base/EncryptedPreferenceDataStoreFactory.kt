@file:Suppress("UnusedReceiverParameter")

package mega.privacy.android.data.preferences.base

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.crypto.tink.StreamingAead
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.monitoring.CrashReporter
import timber.log.Timber

/**
 * Creates Preferences DataStore instance whose file is encrypted with [streamingAead].
 * The file must have the extension "preferences_pb".
 *
 * If no [streamingAead] is available - the Android Keystore being unusable is the usual cause - an
 * unencrypted datastore is used instead. The fallback keeps its data under
 * [unencryptedFallbackFileName] rather than the encrypted file's own path, so an encrypted file
 * is never handed to the plaintext reader, which would be reported as corruption and would reset
 * the store. Each of the two stores adopts the other's contents while it is still empty, so a
 * preference written to one of them is not lost to the other. Once the encrypted store has
 * adopted the fallback, the plaintext file is deleted; the fallback never deletes the encrypted
 * file it adopted from.
 *
 * @see PreferenceDataStoreFactory.create
 */
fun PreferenceDataStoreFactory.createEncrypted(
    context: Context,
    migrations: List<DataMigration<Preferences>> = listOf(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    streamingAead: StreamingAead?,
    fileName: String,
    crashReporter: CrashReporter,
): DataStore<Preferences> {
    val fallbackFileName = unencryptedFallbackFileName(fileName)
    return try {
        val aead = streamingAead ?: throw StreamingAeadUnavailableException(fileName)
        val delegate = DataStoreFactory.createEncrypted(
            serializer = PreferencesSerializer,
            streamingAead = aead,
            corruptionHandler = corruptionHandler(fileName, crashReporter),
            migrations = withAdoptionOf(
                context = context,
                sourceFileName = fallbackFileName,
                sourceDisposal = AdoptedSourceDisposal.DeletePlaintextCopy,
                migrations = migrations,
            ),
            scope = scope,
            produceFile = { context.preferencesDataStoreFile(fileName) },
        )
        PreferenceDataStore(delegate)
    } catch (e: Exception) {
        Timber.e(e, "Falling back to an unencrypted datastore for $fileName")
        crashReporter.report(EncryptedPreferenceFallbackException(fileName, e))
        create(
            corruptionHandler = corruptionHandler(fallbackFileName, crashReporter),
            migrations = withAdoptionOf(
                context = context,
                sourceFileName = fileName,
                sourceDisposal = AdoptedSourceDisposal.KeepEncryptedSource,
                migrations = migrations,
            ),
            scope = scope,
            produceFile = { context.preferencesDataStoreFile(fallbackFileName) }
        )
    }
}

/**
 * Name of the unencrypted file backing [createEncrypted] when the encrypted file cannot be used.
 */
internal fun unencryptedFallbackFileName(fileName: String) = "${fileName}_unencrypted"

/**
 * Adopting the sibling file comes first: it holds whatever was written while this store was
 * unavailable, which is more recent than anything the caller's own migrations can offer.
 */
private fun withAdoptionOf(
    context: Context,
    sourceFileName: String,
    sourceDisposal: AdoptedSourceDisposal,
    migrations: List<DataMigration<Preferences>>,
) = listOf(
    UnencryptedPreferencesFileMigration(context, sourceFileName, sourceDisposal)
) + migrations

private fun corruptionHandler(fileName: String, crashReporter: CrashReporter) =
    ReplaceFileCorruptionHandler<Preferences> { error ->
        Timber.e(error, "Preference datastore $fileName was corrupt and has been reset")
        crashReporter.report(PreferenceDataStoreCorruptionException(fileName, error))
        emptyPreferences()
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
