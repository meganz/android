package mega.privacy.android.app.presentation.notification.model.extensions

import android.content.Context
import mega.privacy.android.app.presentation.notification.model.MAX_WIDTH_FIRST_LINE_NEW_LAND
import mega.privacy.android.app.presentation.notification.model.MAX_WIDTH_FIRST_LINE_NEW_PORT
import mega.privacy.android.app.presentation.notification.model.MAX_WIDTH_FIRST_LINE_SEEN_LAND
import mega.privacy.android.app.presentation.notification.model.MAX_WIDTH_FIRST_LINE_SEEN_PORT
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.UserAlert
import org.jetbrains.anko.configuration
import org.jetbrains.anko.displayMetrics
import org.jetbrains.anko.landscape

/**
 * Title Max Width
 *
 */
internal fun UserAlert.titleMaxWidth(): (Context) -> Int? {
    return { context ->
        var widthInPx: Int? = null
        this.description()(context)?.let {
            widthInPx = if (context.configuration.landscape) {
                MAX_WIDTH_FIRST_LINE_SEEN_LAND
            } else {
                if (!this.seen) {
                    MAX_WIDTH_FIRST_LINE_NEW_PORT
                } else {
                    MAX_WIDTH_FIRST_LINE_SEEN_PORT
                }
            }
        } ?: run {
            widthInPx = if (!this.seen) {
                if (context.configuration.landscape) {
                    MAX_WIDTH_FIRST_LINE_NEW_LAND
                } else {
                    MAX_WIDTH_FIRST_LINE_NEW_PORT
                }
            } else {
                if (context.configuration.landscape) {
                    MAX_WIDTH_FIRST_LINE_SEEN_LAND
                } else {
                    MAX_WIDTH_FIRST_LINE_SEEN_PORT
                }
            }
        }
        widthInPx?.let {
            Util.scaleWidthPx(it, context.displayMetrics)
        }
    }
}