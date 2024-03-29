package mega.privacy.android.data.database.entity.chat

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGifInfo

/**
 * Entity to store a giphy.
 *
 * @property messageId Message ID.
 * @property mp4Src Source of the mp4.
 * @property webpSrc Source of the webp.
 * @property title Title of the giphy.
 * @property mp4Size Size of the mp4.
 * @property webpSize Size of the webp.
 * @property width Width of the giphy.
 * @property height Height of the giphy.
 */
@Entity(tableName = "giphy")
@Serializable
data class GiphyEntity(
    @PrimaryKey val messageId: Long,
    override val mp4Src: String?,
    override val webpSrc: String?,
    override val title: String?,
    override val mp4Size: Int,
    override val webpSize: Int,
    override val width: Int,
    override val height: Int,
) : ChatGifInfo