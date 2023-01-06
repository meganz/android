package mega.privacy.android.app.presentation.notification.model.extensions

import android.content.Context
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.UserAlert
import org.jetbrains.anko.displayMetrics

/**
 * Separator Margin
 *
 */
internal fun UserAlert.separatorMargin(): (Context) -> Int {
    return { context ->
        Util.scaleWidthPx(16, context.displayMetrics)
    }
}