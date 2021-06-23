package mega.privacy.android.app.contacts.group.data

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorInt

data class ContactGroupUser(
    val handle: Long,
    val email: String?,
    val firstName: String?,
    val avatar: Uri?,
    @ColorInt val avatarColor: Int,
    val placeholder: Drawable
)
