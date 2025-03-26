package mega.privacy.android.data.database.spec

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec
import mega.privacy.android.data.database.MegaDatabaseConstant

@DeleteColumn(
    tableName = MegaDatabaseConstant.TABLE_PENDING_TRANSFER,
    columnName = "transferTag"
)
internal class AutoMigrationSpec102to103 : AutoMigrationSpec