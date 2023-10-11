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
        delegate.onOpen(db)
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
}