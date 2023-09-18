package mega.privacy.android.data.database.spec

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn(
    tableName = "active_transfers",
    columnName = "transferred_bytes"
)
internal class AutoMigrationSpec73to74 : AutoMigrationSpec