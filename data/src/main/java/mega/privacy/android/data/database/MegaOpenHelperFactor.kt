package mega.privacy.android.data.database

import androidx.sqlite.db.SupportSQLiteOpenHelper

/**
 * Mega open helper factor
 *
 * @property delegate
 * @property legacyDatabaseMigration
 */
class MegaOpenHelperFactor(
    private val delegate: SupportSQLiteOpenHelper.Factory,
    private val legacyDatabaseMigration: LegacyDatabaseMigration,
) : SupportSQLiteOpenHelper.Factory {
    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val decoratedConfiguration =
            SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
                .name(configuration.name)
                .callback(MegaDatabaseCallback(configuration.callback, legacyDatabaseMigration))
                .build()
        return delegate.create(decoratedConfiguration)
    }
}