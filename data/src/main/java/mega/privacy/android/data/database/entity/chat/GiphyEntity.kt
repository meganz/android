package mega.privacy.android.data.database.entity.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to store a giphy.
 *
 * @property id ID.
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
data class GiphyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val messageId: Long,
    val mp4Src: String?,
    val webpSrc: String?,
    val title: String?,
    val mp4Size: Int,
    val webpSize: Int,
    val width: Int,
    val height: Int,
)