package mega.privacy.android.data.database.chat

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import mega.privacy.android.data.database.chat.spec.AutoMigrationSpecChat2to3
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
 * Chat database name

 */
const val CHAT_DATABASE_NAME = "chat_database"

private const val DATABASE_VERSION = 4

/**
 * In memory chat database
 *
 * An implementation of the chat database
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
    version = DATABASE_VERSION,
    autoMigrations = [
        AutoMigration(1, 2),
        AutoMigration(2, 3, spec = AutoMigrationSpecChat2to3::class),
        AutoMigration(3, 4),
    ],
)
abstract class ChatDatabase : RoomDatabase() {

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

        /**
         * Init
         *
         * @param context
         * @param factory
         * @return Chat database
         */
        fun init(
            context: Context,
            factory: SupportSQLiteOpenHelper.Factory,
        ): ChatDatabase = Room.databaseBuilder(
            context,
            ChatDatabase::class.java,
            CHAT_DATABASE_NAME
        ).openHelperFactory(factory).build()
    }
}
