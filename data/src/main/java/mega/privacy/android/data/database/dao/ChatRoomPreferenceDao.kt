package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.entity.ChatRoomPreferenceEntity

@Dao
internal interface ChatRoomPreferenceDao {
    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_CHAT_ROOM_PREFERENCE} WHERE chatId = :chatId")
    fun getChatRoomPreference(chatId: Long): Flow<ChatRoomPreferenceEntity?>

    @Upsert
    suspend fun upsertChatRoomPreference(entity: ChatRoomPreferenceEntity)
}