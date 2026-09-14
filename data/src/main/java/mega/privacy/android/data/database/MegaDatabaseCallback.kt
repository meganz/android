package mega.privacy.android.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import timber.log.Timber

/**
 * Mega database callback
 *
 * @property delegate
 * @property legacyDatabaseMigration
 */
class MegaDatabaseCallback(
    private val delegate: SupportSQLiteOpenHelper.Callback,
    private val legacyDatabaseMigration: LegacyDatabaseMigration,
) : SupportSQLiteOpenHelper.Callback(delegate.version) {

    override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        delegate.onDowngrade(db, oldVersion, newVersion)
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        Timber.d("onCreate")
        delegate.onCreate(db)
        legacyDatabaseMigration.onCreate(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        Timber.d("onOpen")
        try {
            delegate.onOpen(db)
        } catch (e: IllegalStateException) {
            Timber.e(e, "Room could not verify the database on open, restoring identity")
            restoreIdentity(db, e)
            delegate.onOpen(db)
        }
    }

    override fun onConfigure(db: SupportSQLiteDatabase) {
        delegate.onConfigure(db)
    }

    override fun onCorruption(db: SupportSQLiteDatabase) {
        delegate.onCorruption(db)
    }

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            Timber.d("onUpgrade oldVersion: $oldVersion newVersion: $newVersion")
            legacyDatabaseMigration.onUpgrade(db, oldVersion, newVersion)
            delegate.onUpgrade(db, oldVersion, newVersion)
        } catch (e: Exception) {
            if (oldVersion < 75) {
                // try to run the migration again before we apply room in version 67
                // and the conflict can happen before we migrate to room open helper version 75
                Timber.e(e)
                delegate.onUpgrade(db, 1, 66)
            } else {
                throw e
            }
        }
    }

    /**
     * Room's create path only adds what is missing (`CREATE TABLE IF NOT EXISTS`), rewrites the
     * identity hash and then validates the whole schema, so it restores the identity without
     * touching any intact table. If validation still fails the transaction is rolled back and
     * the original error is rethrown, leaving the data exactly as it was.
     */
    private fun restoreIdentity(db: SupportSQLiteDatabase, cause: IllegalStateException) {
        db.beginTransaction()
        try {
            delegate.onCreate(db)
            db.setTransactionSuccessful()
        } catch (e: IllegalStateException) {
            Timber.e(e, "Schema is still invalid after restoring identity")
            throw cause
        } finally {
            db.endTransaction()
        }
    }
}
