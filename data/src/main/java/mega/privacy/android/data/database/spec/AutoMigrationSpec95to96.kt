package mega.privacy.android.data.database.spec

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec

@DeleteTable(
    tableName = "sdtransfers",
)
internal class AutoMigrationSpec95to96 : AutoMigrationSpec