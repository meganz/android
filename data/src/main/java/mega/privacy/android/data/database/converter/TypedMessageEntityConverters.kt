package mega.privacy.android.data.database.converter

import androidx.room.TypeConverter
import mega.privacy.android.domain.entity.chat.ChatMessageChange

/**
 * Converters for the typed message entity.
 */
class TypedMessageEntityConverters {

    /**
     * Convert a list of longs to a string.
     *
     * @param list List of longs.
     * @return String.
     */
    @TypeConverter
    fun convertFromLongList(list: List<Long>): String = list.joinToString(separator = ",")

    /**
     * Convert a string to a list of longs.
     *
     * @param string String.
     * @return List of longs.
     */
    @TypeConverter
    fun convertToLongList(string: String): List<Long> =
        string.split(",").mapNotNull { it.toLongOrNull() }

    /**
     * Convert a list of strings to a string.
     *
     * @param list List of strings.
     * @return String.
     */
    @TypeConverter
    fun convertFromStringList(list: List<String>): String = list.joinToString(separator = ",")

    /**
     * Convert a string to a list of strings.
     *
     * @param string String.
     * @return List of strings.
     */
    @TypeConverter
    fun convertToStringList(string: String): List<String> =
        string.takeUnless { it.isBlank() }?.split(",") ?: emptyList()

    /**
     * Convert a list of chat message changes to a string.
     *
     * @param list List of chat message changes.
     * @return String.
     */
    @TypeConverter
    fun convertFromChatMessageChangeList(list: List<ChatMessageChange>): String =
        list.joinToString(separator = ",")


    /**
     * Convert to chat message change list
     *
     * @param string
     * @return List of chat message changes.
     */
    @TypeConverter
    fun convertToChatMessageChangeList(string: String): List<ChatMessageChange> =
        string.split(",").mapNotNull { runCatching { ChatMessageChange.valueOf(it) }.getOrNull() }
}