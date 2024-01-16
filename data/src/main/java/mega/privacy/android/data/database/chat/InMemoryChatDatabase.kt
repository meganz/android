package mega.privacy.android.data.database.chat

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mega.privacy.android.data.database.dao.ChatHistoryStateDao
import mega.privacy.android.data.database.dao.ChatMessageMetaDao
import mega.privacy.android.data.database.dao.ChatNodeDao
import mega.privacy.android.data.database.dao.TypedMessageDao
import mega.privacy.android.data.database.entity.chat.ChatGeolocationEntity
import mega.privacy.android.data.database.entity.chat.ChatHistoryLoadStatusEntity
import mega.privacy.android.data.database.entity.chat.ChatNodeEntity
import mega.privacy.android.data.database.entity.chat.GiphyEntity
import mega.privacy.android.data.database.entity.chat.RichPreviewEntity
import mega.privacy.android.data.database.entity.chat.TypedMessageEntity

/**
 * In memory chat database
 *
 * An in memory implementation of the chat database
 */
@Database(
    entities = [
        TypedMessageEntity::class,
        ChatHistoryLoadStatusEntity::class,
        RichPreviewEntity::class,
        GiphyEntity::class,
        ChatGeolocationEntity::class,
        ChatNodeEntity::class,
    ],
    version = 1,
)
internal abstract class InMemoryChatDatabase : RoomDatabase() {

    abstract fun typedMessageRequestDao(): TypedMessageDao

    abstract fun chatHistoryStateDao(): ChatHistoryStateDao

    abstract fun chatMessageMetaDao(): ChatMessageMetaDao

    abstract fun chatNodeDao(): ChatNodeDao

    companion object {
        fun create(context: Context): InMemoryChatDatabase {
            return Room.inMemoryDatabaseBuilder(
                context,
                InMemoryChatDatabase::class.java
            ).build()
        }
    }
}