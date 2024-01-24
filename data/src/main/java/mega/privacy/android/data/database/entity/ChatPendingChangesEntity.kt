package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

@Entity(tableName = MegaDatabaseConstant.TABLE_CHAT_ROOM_PREFERENCE)
internal data class ChatPendingChangesEntity(
    @PrimaryKey
    val chatId: Long = 0L,
    @ColumnInfo(name = "draft_message")
    val draftMessage: String = "",
)