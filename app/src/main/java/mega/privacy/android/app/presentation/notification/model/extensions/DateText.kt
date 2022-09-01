package mega.privacy.android.app.presentation.notification.model.extensions

import android.content.Context
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.UserAlert

/**
 * Date text
 *
 */
internal fun UserAlert.dateText(): (Context) -> String {
    return { context ->
        TimeUtils.formatDateAndTime(context, this.createdTime, TimeUtils.DATE_LONG_FORMAT)
    }
}