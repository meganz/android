package mega.privacy.android.app.presentation.notification.model.extensions

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.Contact

/**
 * Get nickname string or email
 *
 * @param context
 */
internal fun Contact.getNicknameStringOrEmail(context: Context): String {
    val emailOrDefault = email ?: context.getString(R.string.unknown_name_label)
    return nickname?.let {
        String.format(
            format = context.getString(R.string.location_label),
            it,
            emailOrDefault
        )
    } ?: emailOrDefault
}