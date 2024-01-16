package mega.privacy.android.data.database.entity.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to store a rich preview.
 *
 * @property id ID.
 * @property messageId Message ID.
 * @property title Title.
 * @property description Description.
 * @property image Image.
 * @property imageFormat Image format.
 * @property icon Icon.
 * @property iconFormat Icon format.
 * @property url URL.
 * @property domainName Domain name.
 */
@Entity(tableName = "rich_preview")
data class RichPreviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val messageId: Long,
    val title: String,
    val description: String,
    val image: String?,
    val imageFormat: String?,
    val icon: String?,
    val iconFormat: String?,
    val url: String,
    val domainName: String,
)