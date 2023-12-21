package mega.privacy.android.data.database

import android.content.Context
import androidx.security.crypto.EncryptedFile
import dagger.hilt.android.qualifiers.ApplicationContext
import net.sqlcipher.database.SQLiteDatabase
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
    private val passphraseEncryptedFile: EncryptedFile?,
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
        }
    }

    private fun getDatabaseState(context: Context, dbPath: File): DatabaseState {
        SQLiteDatabase.loadLibs(context)

        if (dbPath.exists()) {
            var db: SQLiteDatabase? = null

            return try {
                db = SQLiteDatabase.openDatabase(
                    dbPath.absolutePath,
                    "",
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
        SQLiteDatabase.loadLibs(context)

        if (originalFile.exists()) {
            val originalDb = SQLiteDatabase.openDatabase(
                originalFile.absolutePath,
                "",
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
            val version = originalDb.version

            originalDb.close()

            val db = SQLiteDatabase.openOrCreateDatabase(
                targetFile.absolutePath,
                passphrase,
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
            passphraseEncryptedFile?.let { encryptedFile ->
                Timber.d("Reading passphrase from encrypted file")
                encryptedFile.openFileInput().use { it.readBytes() }
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
            passphraseEncryptedFile?.let { encryptedFile ->
                Timber.d("Writing passphrase to encrypted file")
                encryptedFile.openFileOutput().use { it.write(result) }
            } ?: run {
                Timber.d("Writing passphrase to file")
                passphraseFile.writeBytes(result)
            }
            result
        }
    }

    companion object {
        private const val PASSPHRASE_LENGTH = 32
    }
}
