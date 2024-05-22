package mega.privacy.android.app.contacts.requests.mapper

import android.graphics.drawable.Drawable
import android.net.Uri
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.domain.entity.contacts.ContactRequest
import javax.inject.Inject


internal class ContactRequestItemMapper @Inject constructor(
    private val formatCreationTime: (Long) -> String,
) {
    operator fun invoke(
        request: ContactRequest,
        avatarUri: Uri?,
        placeHolder: Drawable,
    ): ContactRequestItem? = with(request) {
        val email = (if (isOutgoing) targetEmail else sourceEmail) ?: return null
        ContactRequestItem(
            handle = handle,
            email = email,
            avatarUri = avatarUri,
            placeholder = placeHolder,
            createdTime = formatCreationTime(creationTime),
            isOutgoing = isOutgoing,
        )
    }
}