package mega.privacy.android.app.presentation.notification.model.extensions

import androidx.compose.ui.unit.dp
import mega.privacy.android.domain.entity.CustomAlert
import mega.privacy.android.domain.entity.IncomingShareAlert
import mega.privacy.android.domain.entity.UserAlert

/**
 * Title text size
 *
 */
internal fun UserAlert.titleTextSize() = when (this) {
    is CustomAlert -> 14f
    is IncomingShareAlert -> 14f
    else -> 16f
}

