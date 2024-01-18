package mega.privacy.android.data.database.entity.chat

import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.domain.entity.chat.ChatGeolocationInfo

/**
 * Entity to store a geolocation message.
 *
 * @property id ID.
 * @property messageId Message ID.
 * @property longitude Longitude.
 * @property latitude Latitude.
 * @property image Image.
 */
@Entity(tableName = "chat_geolocation")
data class ChatGeolocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val messageId: Long,
    override val longitude: Float,
    override val latitude: Float,
    override val image: String?,
) : ChatGeolocationInfo