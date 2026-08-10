package mega.privacy.android.data.database.spec

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_ACTIVE_TRANSFERS_LEGACY

@DeleteTable(
    tableName = TABLE_ACTIVE_TRANSFERS_LEGACY,
)
internal class AutoMigrationDeleteActiveTransfersSpec : AutoMigrationSpec