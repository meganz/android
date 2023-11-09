package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

@Entity(
    tableName = MegaDatabaseConstant.TABLE_SYNC_SOLVED_ISSUES
)
data class SyncSolvedIssueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "entityId")
    val entityId: Int? = null,
    @ColumnInfo(name = "nodeIds")
    val nodeIds: String,
    @ColumnInfo(name = "localPaths")
    val localPaths: String,
    @ColumnInfo(name = "resolutionExplanation")
    val resolutionExplanation: String,
)