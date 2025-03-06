package mega.privacy.android.data.database.spec

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec
import mega.privacy.android.data.database.MegaDatabaseConstant

@DeleteColumn(
    tableName = MegaDatabaseConstant.TABLE_ACTIVE_TRANSFER_ACTION_GROUPS,
    columnName = "fileName"
)
internal class AutoMigrationSpec100to101 : AutoMigrationSpec