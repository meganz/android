package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * Sync solved issue entity for Room database
 *
 * @property entityId Room database entity id
 * @property syncId sync id of the Sync associated with this issue
 * @property nodeIds list of nodeIds involved in the issue
 * @property localPaths list of paths of device storage involved in the issue
 * @property resolutionExplanation explanation of how the issue was resolved
 */
@Entity(
    tableName = MegaDatabaseConstant.TABLE_SYNC_SOLVED_ISSUES
)
data class SyncSolvedIssueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "entityId")
    val entityId: Int? = null,
    @ColumnInfo(name = "syncId", defaultValue = "-1")
    val syncId: Long,
    @ColumnInfo(name = "nodeIds")
    val nodeIds: String,
    @ColumnInfo(name = "localPaths")
    val localPaths: String,
    @ColumnInfo(name = "resolutionExplanation")
    val resolutionExplanation: String,
)