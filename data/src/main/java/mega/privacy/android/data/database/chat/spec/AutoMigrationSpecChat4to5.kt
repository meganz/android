package mega.privacy.android.data.database.chat.spec

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn(
    tableName = "pending_messages",
    columnName = "transferTag"
)
internal class AutoMigrationSpecChat4to5 : AutoMigrationSpec
