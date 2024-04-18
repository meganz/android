package mega.privacy.android.data.database.chat.spec

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn(
    tableName = "typed_messages",
    columnName = "shouldShowAvatar"
)
internal class AutoMigrationSpecChat2to3 : AutoMigrationSpec
