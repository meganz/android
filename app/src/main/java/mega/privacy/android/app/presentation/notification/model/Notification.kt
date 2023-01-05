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
 * @property titleMaxWidth
 * @property description
 * @property descriptionMaxWidth
 * @property dateText
 * @property isNew
 * @property backgroundColor
 * @property separatorMargin
 * @property onClick
 * @constructor Create empty Notification
 */
data class Notification(
    val sectionTitle: (Context) -> String,
    @ColorRes val sectionColour: Int,
    @DrawableRes val sectionIcon: Int?,
    val title: (Context) -> CharSequence,
    val titleTextSize: Dp,
    val titleMaxWidth: (Context) -> Int?,
    val description: (Context) -> CharSequence?,
    val descriptionMaxWidth: (Context) -> Int?,
    val dateText: (Context) -> String,
    val isNew: Boolean,
    val backgroundColor: (Context) -> String,
    val separatorMargin: (Context) -> Int,
    val onClick: (NotificationNavigationHandler) -> Unit,
)