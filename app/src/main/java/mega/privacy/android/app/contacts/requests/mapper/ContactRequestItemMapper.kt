package mega.privacy.android.app.contacts.requests.mapper

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.format.DateUtils.getRelativeTimeSpanString
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.view.TextDrawable
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.repository.AvatarRepository
import javax.inject.Inject


internal class ContactRequestItemMapper(
    private val formatCreationTime: (Long) -> CharSequence,
    private val avatarRepository: AvatarRepository,
    private val getImagePlaceholder: (String, Int) -> Drawable,
) {

    @Inject
    constructor(
        avatarRepository: AvatarRepository,
        @ApplicationContext context: Context,
    ) : this(
        formatCreationTime = { getRelativeTimeSpanString(it) },
        avatarRepository = avatarRepository,
        getImagePlaceholder = { title, color -> context.getImagePlaceholder(title, color) },
    )

    suspend operator fun invoke(
        request: ContactRequest,
    ): ContactRequestItem? = with(request) {

        val email = (if (isOutgoing) targetEmail else sourceEmail) ?: return null
        val userImageColor = avatarRepository.getAvatarColor(handle)
        val placeholder = getImagePlaceholder(email, userImageColor)
        val userImageUri = runCatching {
            avatarRepository.getAvatarFile(email)
        }.getOrNull()?.takeIf { it.exists() }?.toUri()

        ContactRequestItem(
            handle = handle,
            email = email,
            avatarUri = userImageUri,
            placeholder = placeholder,
            createdTime = formatCreationTime(creationTime * 1000).toString(),
            isOutgoing = isOutgoing,
        )
    }
}

private fun Context.getImagePlaceholder(title: String, @ColorInt color: Int): Drawable =
    TextDrawable.builder()
        .beginConfig()
        .width(resources.getDimensionPixelSize(R.dimen.image_contact_size))
        .height(resources.getDimensionPixelSize(R.dimen.image_contact_size))
        .fontSize(resources.getDimensionPixelSize(R.dimen.image_contact_text_size))
        .textColor(ContextCompat.getColor(this, R.color.white))
        .bold()
        .toUpperCase()
        .endConfig()
        .buildRound(AvatarUtil.getFirstLetter(title), color)