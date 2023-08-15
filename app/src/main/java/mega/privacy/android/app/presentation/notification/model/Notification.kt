package mega.privacy.android.app.presentation.notification.model

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.TextUnit

/**
 * Notification
 *
 * @property sectionTitle
 * @property sectionColour
 * @property sectionIcon
 * @property title
 * @property titleTextSize
 * @property description
 * @property schedMeetingNotification
 * @property dateText
 * @property isNew
 * @property backgroundColor
 * @property separatorMargin
 * @property onClick
 * @constructor Create empty Notification
 */
data class Notification constructor(
    val sectionTitle: (Context) -> String,
    @ColorRes val sectionColour: Int,
    @DrawableRes val sectionIcon: Int?,
    val title: (Context) -> String,
    val titleTextSize: TextUnit,
    val description: (Context) -> String?,
    val schedMeetingNotification: SchedMeetingNotification?,
    val dateText: (Context) -> String,
    val isNew: Boolean,
    val backgroundColor: (Context) -> String,
    val separatorMargin: (Context) -> Int,
    val onClick: (NotificationNavigationHandler) -> Unit,
)
