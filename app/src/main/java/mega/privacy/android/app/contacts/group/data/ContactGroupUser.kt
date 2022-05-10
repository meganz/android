package mega.privacy.android.app.contacts.group.data

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.view.TextDrawable

/**
 * View item that represents a Contact Group User at UI level.
 *
 * @property handle         User handle
 * @property email          User email
 * @property firstName      User first name
 * @property avatar         User avatar Uri
 * @property avatarColor    User avatar color
 */
data class ContactGroupUser(
    val handle: Long,
    val email: String?,
    val firstName: String?,
    val avatar: Uri?,
    @ColorInt val avatarColor: Int
) {

    /**
     * Build Avatar placeholder Drawable given a Title and a Color
     *
     * @param context   Activity context
     * @return          Drawable with the placeholder
     */
    fun getImagePlaceholder(context: Context): Drawable =
        TextDrawable.builder()
            .beginConfig()
            .width(context.resources.getDimensionPixelSize(R.dimen.image_group_size))
            .height(context.resources.getDimensionPixelSize(R.dimen.image_group_size))
            .fontSize(context.resources.getDimensionPixelSize(R.dimen.image_group_text_size))
            .withBorder(context.resources.getDimensionPixelSize(R.dimen.image_group_border_size))
            .borderColor(ContextCompat.getColor(context, R.color.white_dark_grey))
            .bold()
            .toUpperCase()
            .endConfig()
            .buildRound(
                AvatarUtil.getFirstLetter(firstName ?: email ?: handle.toString()),
                avatarColor
            )
}
