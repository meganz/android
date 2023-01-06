package mega.privacy.android.app.presentation.notification.model.extensions

import android.content.Context
import mega.privacy.android.app.presentation.notification.model.MAX_WIDTH_FIRST_LINE_SEEN_LAND
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.UserAlert
import org.jetbrains.anko.displayMetrics

/**
 * Description Max Width
 *
 */
internal fun UserAlert.descriptionMaxWidth(): (Context) -> Int? {
    return { context ->
        var widthInPx: Int? = null
        this.description()(context)?.let {
            widthInPx = MAX_WIDTH_FIRST_LINE_SEEN_LAND
        }
        widthInPx?.let {
            Util.scaleWidthPx(it, context.displayMetrics)
        }
    }
}