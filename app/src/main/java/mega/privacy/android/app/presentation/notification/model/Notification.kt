package mega.privacy.android.app.presentation.notification.model

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.Dp

/**
 * Notification
 *
 * @property sectionTitle
 * @property sectionColour
 * @property sectionIcon
 * @property title
 * @property titleTextSize
 * @property description
 * @property dateText
 * @property isNew
 * @property backgroundColor
 * @property onClick
 * @constructor Create empty Notification
 */
data class Notification(
    val sectionTitle: (Context) -> String,
    @ColorRes val sectionColour: Int,
    @DrawableRes val sectionIcon: Int?,
    val title: (Context) -> CharSequence,
    val titleTextSize: Dp,
    val description: (Context) -> CharSequence?,
    val dateText: (Context) -> String,
    val isNew: Boolean,
    val backgroundColor: (Context) -> String,
    val onClick: (NotificationNavigationHandler) -> Unit,
)