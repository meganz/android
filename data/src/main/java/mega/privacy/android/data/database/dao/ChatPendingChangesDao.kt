package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.entity.ChatPendingChangesEntity

@Dao
internal interface ChatPendingChangesDao {
    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_CHAT_ROOM_PREFERENCE} WHERE chatId = :chatId")
    fun getChatPendingChanges(chatId: Long): Flow<ChatPendingChangesEntity?>

    @Upsert
    suspend fun upsertChatPendingChanges(entity: ChatPendingChangesEntity)
}