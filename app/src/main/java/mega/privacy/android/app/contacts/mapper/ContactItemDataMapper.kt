package mega.privacy.android.app.contacts.mapper

import mega.privacy.android.app.contacts.list.data.ContactItem as UIContactItem
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.MegaUserUtils.getUserStatusColor
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.view.TextDrawable
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import nz.mega.sdk.MegaChatApi
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

internal class ContactItemDataMapper(
    private val getUnformattedLastSeenDate: (Int) -> String,
    private val getPlaceHolderDrawable: (title: String, colour: Int) -> Drawable,
    private val wasRecentlyAdded: (Long) -> Boolean,
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(
        getUnformattedLastSeenDate = { lastSeen ->
            TimeUtils.unformattedLastGreenDate(
                context,
                lastSeen
            )
        },
        getPlaceHolderDrawable = { title: String, colour: Int ->
            getImagePlaceholder(
                context,
                title,
                colour
            )
        },
        wasRecentlyAdded = { timestamp ->
            isMoreThanThreeDaysAgo(timestamp)
        }
    )

    operator fun invoke(
        contactItem: ContactItem,
    ): UIContactItem.Data {
        val status = getStatusInt(contactItem.status)
        val fullName = contactItem.contactData.fullName
        val alias = contactItem.contactData.alias
        val email = contactItem.email
        val userImageColor = contactItem.defaultAvatarColor?.toColorInt() ?: -1
        val title = when {
            !alias.isNullOrBlank() -> alias
            !fullName.isNullOrBlank() -> fullName
            else -> email
        }
        return UIContactItem.Data(
            handle = contactItem.handle,
            email = email,
            fullName = fullName,
            alias = alias,
            status = status,
            statusColor = getUserStatusColor(status),
            avatarUri = contactItem.contactData.avatarUri?.toUri(),
            placeholder = getPlaceHolderDrawable(title, userImageColor),
            lastSeen = contactItem.lastSeen?.let { getUnformattedLastSeenDate(it) },
            isNew = wasRecentlyAdded(contactItem.timestamp) && contactItem.chatroomId == null,
            isVerified = contactItem.areCredentialsVerified,
        )
    }

    private fun getStatusInt(status: UserChatStatus): Int {
        return when (status) {
            UserChatStatus.Online -> MegaChatApi.STATUS_ONLINE
            UserChatStatus.Away -> MegaChatApi.STATUS_AWAY
            UserChatStatus.Busy -> MegaChatApi.STATUS_BUSY
            UserChatStatus.Offline -> MegaChatApi.STATUS_OFFLINE
            UserChatStatus.Invalid -> MegaChatApi.STATUS_INVALID
        }
    }

}

private fun getImagePlaceholder(
    context: Context,
    title: String,
    @ColorInt color: Int,
): Drawable =
    TextDrawable.builder()
        .beginConfig()
        .width(context.resources.getDimensionPixelSize(R.dimen.image_contact_size))
        .height(context.resources.getDimensionPixelSize(R.dimen.image_contact_size))
        .fontSize(context.resources.getDimensionPixelSize(R.dimen.image_contact_text_size))
        .textColor(ContextCompat.getColor(context, R.color.white))
        .bold()
        .toUpperCase()
        .endConfig()
        .buildRound(AvatarUtil.getFirstLetter(title), color)

private fun isMoreThanThreeDaysAgo(timestamp: Long): Boolean {
    val now = LocalDateTime.now()
    val addedTime = Instant.ofEpochSecond(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return Duration.between(addedTime, now).toDays() < 3
}
