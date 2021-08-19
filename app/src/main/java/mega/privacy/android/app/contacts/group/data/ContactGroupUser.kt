package mega.privacy.android.app.contacts.group.data

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorInt

/**
 * View item that represents a Contact Group User at UI level.
 *
 * @property handle         User handle
 * @property email          User email
 * @property firstName      User first name
 * @property avatar         User avatar Uri
 * @property avatarColor    User avatar color
 * @property placeholder    User avatar placeholder
 */
data class ContactGroupUser(
    val handle: Long,
    val email: String?,
    val firstName: String?,
    val avatar: Uri?,
    @ColorInt val avatarColor: Int,
    val placeholder: Drawable
)
