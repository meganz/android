package mega.privacy.android.app.presentation.notification.model.extensions

import androidx.annotation.ColorRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.ContactAlert
import mega.privacy.android.domain.entity.CustomAlert
import mega.privacy.android.domain.entity.IncomingShareAlert
import mega.privacy.android.domain.entity.UserAlert

@ColorRes
internal fun UserAlert.sectionColour() = when (this) {
    is ContactAlert -> R.color.jade_600_jade_300
    is IncomingShareAlert -> R.color.orange_400_orange_300
    is CustomAlert -> R.color.red_600_red_300
    else -> R.color.orange_400_orange_300
}