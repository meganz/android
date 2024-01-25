package mega.privacy.android.data.database.entity.chat

import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.domain.entity.chat.messages.ChatRichPreviewInfo

/**
 * Entity to store a rich preview.
 *
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
    @PrimaryKey val messageId: Long,
    override val title: String,
    override val description: String,
    override val image: String?,
    override val imageFormat: String?,
    override val icon: String?,
    override val iconFormat: String?,
    override val url: String,
    override val domainName: String,
) : ChatRichPreviewInfo