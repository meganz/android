package mega.privacy.android.data.database.chat

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mega.privacy.android.data.database.dao.ChatMessageMetaDao
import mega.privacy.android.data.database.dao.ChatNodeDao
import mega.privacy.android.data.database.dao.PendingMessageDao
import mega.privacy.android.data.database.dao.TypedMessageDao
import mega.privacy.android.data.database.entity.chat.ChatGeolocationEntity
import mega.privacy.android.data.database.entity.chat.ChatNodeEntity
import mega.privacy.android.data.database.entity.chat.GiphyEntity
import mega.privacy.android.data.database.entity.chat.NodeMessageCrossRef
import mega.privacy.android.data.database.entity.chat.PendingMessageEntity
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
        RichPreviewEntity::class,
        GiphyEntity::class,
        ChatGeolocationEntity::class,
        ChatNodeEntity::class,
        PendingMessageEntity::class,
        NodeMessageCrossRef::class,
    ],
    version = 1,
)
abstract class InMemoryChatDatabase : RoomDatabase() {

    /**
     * Typed message dao
     */
    abstract fun typedMessageDao(): TypedMessageDao

    /**
     * Chat message meta dao
     */
    abstract fun chatMessageMetaDao(): ChatMessageMetaDao

    /**
     * Chat node dao
     */
    abstract fun chatNodeDao(): ChatNodeDao

    /**
     * Pending message dao
     */
    abstract fun pendingMessageDao(): PendingMessageDao

    companion object {
        fun create(context: Context): InMemoryChatDatabase {
            return Room.inMemoryDatabaseBuilder(
                context,
                InMemoryChatDatabase::class.java
            ).build()
        }
    }
}