package mega.privacy.android.app.presentation.notification.model.extensions

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.domain.entity.UserAlert

/**
 * Background Color
 *
 */
internal fun UserAlert.backgroundColor(): (Context) -> String {
    return { context ->
        if (this.seen)
            ColorUtils.getColorHexString(context,
                R.color.grey_020_grey_800)
        else
            ColorUtils.getThemeColorHexString(
                context,
                android.R.attr.colorBackground)
    }
}