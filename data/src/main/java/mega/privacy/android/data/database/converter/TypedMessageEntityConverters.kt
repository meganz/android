package mega.privacy.android.data.database.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import kotlinx.serialization.json.Json
import mega.privacy.android.domain.entity.chat.ChatMessageChange
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Converters for the typed message entity.
 */
class TypedMessageEntityConverters @Inject constructor() {

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

    /**
     * Convert a [Duration] to Long.
     *
     * @param duration [Duration].
     * @return [Long].
     */
    @TypeConverter
    fun convertFromDuration(duration: Duration): Long = duration.inWholeSeconds

    /**
     * Convert to Duration.
     *
     * @param long [Long].
     * @return [Duration].
     */
    @TypeConverter
    fun convertToDuration(long: Long): Duration = long.seconds

    /**
     * Convert a list of [Reaction] to String.
     *
     * @param list List of [Reaction].
     * @return [String].
     */
    @TypeConverter
    fun convertFromMessageReactionList(list: List<Reaction>): String =
        Json.encodeToString(list)

    /**
     * Convert String to a list of [Reaction]
     *
     * @param string [String].
     * @return list List of [Reaction].
     */
    @TypeConverter
    fun convertToMessageReactionList(string: String): List<Reaction> = when {
        string.isBlank() -> emptyList()
        string.startsWith("[") -> runCatching {
            Json.decodeFromString<List<Reaction>>(string)
        }.getOrDefault(emptyList())

        else -> convertLegacyMessageReactionList(string)
    }

    private fun convertLegacyMessageReactionList(string: String): List<Reaction> =
        string.split(";").mapNotNull { fragment ->
            val legacy = runCatching {
                Gson().fromJson(fragment, LegacyReaction::class.java)
            }.getOrNull() ?: return@mapNotNull null
            Reaction(
                reaction = legacy.reaction ?: return@mapNotNull null,
                count = legacy.count ?: 0,
                userHandles = legacy.userHandles ?: return@mapNotNull null,
                hasMe = legacy.hasMe ?: false,
            )
        }
}

/**
 * Nullable mirror of [Reaction] for reading rows persisted with Gson by older app versions.
 *
 * Gson instantiates without running constructors, so fields whose JSON keys are missing
 * (e.g. written with obfuscated names before the Reaction keep rule existed) stay null even
 * though [Reaction] declares them non-null. Deserialising into this type instead makes those
 * nulls visible so corrupt entries can be dropped rather than crash later when hashed.
 */
private data class LegacyReaction(
    val reaction: String?,
    val count: Int?,
    val userHandles: List<Long>?,
    val hasMe: Boolean?,
)