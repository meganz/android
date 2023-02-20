package mega.privacy.android.app.presentation.notification.model

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting

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
 * @property chatDateText
 * @property recurringSchedMeeting
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
    val title: (Context) -> CharSequence,
    val titleTextSize: Dp,
    val titleMaxWidth: (Context) -> Int?,
    val description: (Context) -> CharSequence?,
    val descriptionMaxWidth: (Context) -> Int?,
    val chatDateText: (Context) -> AnnotatedString?,
    val recurringSchedMeeting: ChatScheduledMeeting?,
    val dateText: (Context) -> String,
    val isNew: Boolean,
    val backgroundColor: (Context) -> String,
    val separatorMargin: (Context) -> Int,
    val onClick: (NotificationNavigationHandler) -> Unit,
)