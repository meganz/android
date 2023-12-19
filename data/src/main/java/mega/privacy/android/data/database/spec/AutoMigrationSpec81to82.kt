package mega.privacy.android.data.database.spec

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec

@DeleteTable(
    tableName = "syncrecords",
)
internal class AutoMigrationSpec81to82 : AutoMigrationSpec
