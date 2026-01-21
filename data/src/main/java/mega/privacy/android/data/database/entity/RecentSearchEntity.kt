package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

@Entity(tableName = MegaDatabaseConstant.TABLE_RECENT_SEARCH)
internal data class RecentSearchEntity(
    @PrimaryKey
    val searchQuery: String = "",

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = 0L
)

