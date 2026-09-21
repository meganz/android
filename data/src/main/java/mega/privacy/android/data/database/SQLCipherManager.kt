package mega.privacy.android.data.database

import android.content.Context
import com.google.crypto.tink.StreamingAead
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.cryptography.FileStreamingAeadFactory
import net.zetetic.database.sqlcipher.SQLiteDatabase
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.security.SecureRandom
import javax.inject.Inject

/**
 * The detected state of the database, based on whether we can open it
 * without a passphrase.
 */
internal enum class DatabaseState {
    DOES_NOT_EXIST, UNENCRYPTED, ENCRYPTED
}

internal data class SQLCipherManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val passphraseFile: File,
    private val streamingAead: StreamingAead?,
) {

    fun migrateToSecureDatabase(name: String, passphrase: ByteArray) {
        val dbFile = context.getDatabasePath(name)
        val state = getDatabaseState(context, dbFile)

        if (state == DatabaseState.UNENCRYPTED) {
            val dbTemp = context.getDatabasePath("_temp.db")

            dbTemp.delete()

            encryptTo(context, dbFile, dbTemp, passphrase)

            val dbBackup = context.getDatabasePath("_backup.db")

            if (dbFile.renameTo(dbBackup)) {
                if (dbTemp.renameTo(dbFile)) {
                    dbBackup.delete()
                } else {
                    dbBackup.renameTo(dbFile)
                    throw IOException("Could not rename $dbTemp to $dbFile")
                }
            } else {
                dbTemp.delete()
                throw IOException("Could not rename $dbFile to $dbBackup")
            }
        } else if (state == DatabaseState.ENCRYPTED) {
            // ensure passphrase is correct
            var db: SQLiteDatabase? = null
            try {
                db = SQLiteDatabase.openOrCreateDatabase(
                    dbFile.absolutePath,
                    passphrase,
                    null,
                    null,
                )
                db.version
            } finally {
                db?.close()
            }
        }
    }

    /**
     * Destruct secure database if it's encrypted.
     *
     * @param name
     */
    fun destructSecureDatabase(name: String) {
        runCatching {
            val dbFile = context.getDatabasePath(name)
            val state = getDatabaseState(context, dbFile)
            if (state != DatabaseState.UNENCRYPTED) {
                dbFile.delete()
            }
        }.onFailure {
            Timber.e(it, "Failed to destruct secure database")
        }
    }

    private fun getDatabaseState(context: Context, dbPath: File): DatabaseState {
        System.loadLibrary("sqlcipher");

        if (dbPath.exists()) {
            var db: SQLiteDatabase? = null

            return try {
                db = SQLiteDatabase.openDatabase(
                    dbPath.absolutePath,
                    //"", do not need password for open unencrypted db for sqlcipher-android
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                db.version
                DatabaseState.UNENCRYPTED
            } catch (e: Exception) {
                DatabaseState.ENCRYPTED
            } finally {
                db?.close()
            }
        }

        return DatabaseState.DOES_NOT_EXIST
    }

    private fun encryptTo(
        context: Context,
        originalFile: File,
        targetFile: File,
        passphrase: ByteArray?,
    ) {
        System.loadLibrary("sqlcipher");

        if (originalFile.exists()) {
            val originalDb = SQLiteDatabase.openDatabase(
                originalFile.absolutePath,
                //"", do not need password for open unencrypted db for sqlcipher-android
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
            val version = originalDb.version

            originalDb.close()

            val db = SQLiteDatabase.openOrCreateDatabase(
                targetFile.absolutePath,
                passphrase,
                null,
                null
            )

            //language=text
            val st = db.compileStatement("ATTACH DATABASE ? AS plaintext KEY ''")

            st.bindString(1, originalFile.absolutePath)
            st.execute()
            db.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')")
            db.rawExecSQL("DETACH DATABASE plaintext")
            db.version = version
            st.close()
            db.close()
        } else {
            throw FileNotFoundException(originalFile.absolutePath + " not found")
        }
    }

    fun getPassphrase(): ByteArray {
        return if (passphraseFile.exists()) {
            Timber.d("Passphrase file exists")
            streamingAead?.let { aead ->
                Timber.d("Reading passphrase from encrypted file")
                readEncryptedPassphrase(aead)
            } ?: run {
                Timber.d("Reading passphrase from file")
                passphraseFile.readBytes()
            }
        } else {
            val random = SecureRandom.getInstanceStrong()
            val result = ByteArray(PASSPHRASE_LENGTH)

            random.nextBytes(result)

            // https://discuss.zetetic.net/t/technical-guidance-using-random-values-as-sqlcipher-keys/3715
            // Zetetic requires binary keys to not have zero byte values.
            while (result.contains(0)) {
                Timber.d("Contains 0, generating again")
                random.nextBytes(result)
            }
            Timber.d("Passphrase file does not exist")
            streamingAead?.let { aead ->
                Timber.d("Writing passphrase to encrypted file")
                aead.newEncryptingStream(passphraseFile.outputStream(), passphraseAssociatedData)
                    .use {
                        it.write(result)
                        it.flush()
                    }
            } ?: run {
                Timber.d("Writing passphrase to file")
                passphraseFile.outputStream().use {
                    it.write(result)
                    it.flush()
                }
            }
            result
        }
    }

    /**
     * A passphrase written while encryption was unavailable is stored as the raw
     * [PASSPHRASE_LENGTH] bytes, which is shorter than any valid ciphertext, so it is still
     * readable once encryption becomes available on the device.
     */
    private fun readEncryptedPassphrase(aead: StreamingAead): ByteArray = runCatching {
        aead.newDecryptingStream(passphraseFile.inputStream(), passphraseAssociatedData)
            .use { it.readBytes() }
    }.getOrElse { error ->
        if (passphraseFile.length() != PASSPHRASE_LENGTH.toLong()) throw error
        Timber.w(error, "Passphrase file is not encrypted, reading it as plaintext")
        passphraseFile.readBytes()
    }

    private val passphraseAssociatedData: ByteArray
        get() = FileStreamingAeadFactory.associatedData(passphraseFile.name)

    companion object {
        private const val PASSPHRASE_LENGTH = 32
    }
}
