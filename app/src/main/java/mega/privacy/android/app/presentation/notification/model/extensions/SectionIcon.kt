package mega.privacy.android.app.presentation.notification.model.extensions

import androidx.annotation.DrawableRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.IncomingShareAlert
import mega.privacy.android.domain.entity.UserAlert

@DrawableRes
internal fun UserAlert.sectionIcon() = when (this) {
    is IncomingShareAlert -> R.drawable.ic_y_arrow_in
    else -> null
}