package mega.privacy.android.data.preferences.base

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import timber.log.Timber

/**
 * What the store that adopted a file's contents does with that file once the values are safely
 * held in the store's own file.
 */
internal enum class AdoptedSourceDisposal {

    /**
     * The source is a plaintext file and the adopting store is encrypted, so the source is
     * removed. Leaving it behind would keep credentials and passcodes readable on disk for as
     * long as the app is installed.
     */
    DeletePlaintextCopy,

    /**
     * The source is the encrypted file, which is never removed: it stays the only copy of
     * anything written to it, and nothing is gained by destroying ciphertext.
     */
    KeepEncryptedSource,
}

/**
 * Copies the contents of the unencrypted preferences file named [sourceFileName] into a store
 * that is still empty, then disposes of that file as [sourceDisposal] says.
 *
 * An encrypted preference store and its unencrypted fallback keep separate files, so either of
 * them can end up holding the only copy of a preference. Reading the other file while this store
 * has nothing of its own is always a recovery: no value is overwritten. A file that does not
 * parse as unencrypted preferences - an encrypted file above all - is ignored rather than treated
 * as corruption, and is never disposed of.
 */
internal class UnencryptedPreferencesFileMigration(
    private val context: Context,
    private val sourceFileName: String,
    private val sourceDisposal: AdoptedSourceDisposal,
) : DataMigration<Preferences> {

    @Volatile
    private var adopted = false

    override suspend fun shouldMigrate(currentData: Preferences) =
        currentData.asMap().isEmpty() && sourceFile().exists()

    override suspend fun migrate(currentData: Preferences): Preferences {
        val source = runCatching {
            sourceFile().inputStream().use { PreferencesSerializer.readFrom(it) }
        }.onFailure {
            Timber.i(it, "$sourceFileName is not readable as unencrypted preferences")
        }.getOrNull() ?: return currentData

        adopted = true
        return currentData.toMutablePreferences().apply { this += source }
    }

    /**
     * DataStore runs this once the adopted values have been written, and runs it whenever
     * [shouldMigrate] was true - [migrate] having found nothing it could read included - so the
     * adoption is confirmed here before anything is removed. A failure to remove the file is not
     * allowed to escape: DataStore rethrows what clean up throws, which would leave the store
     * uncreatable.
     */
    override suspend fun cleanUp() {
        if (!adopted || sourceDisposal == AdoptedSourceDisposal.KeepEncryptedSource) return

        val source = sourceFile()
        runCatching { source.delete() }
            .onFailure { Timber.w(it, "Deleting $sourceFileName failed") }
        if (runCatching { source.exists() }.getOrDefault(false)) {
            Timber.w("$sourceFileName still holds a plaintext copy of the adopted preferences")
        }
    }

    private fun sourceFile() = context.preferencesDataStoreFile(sourceFileName)
}
