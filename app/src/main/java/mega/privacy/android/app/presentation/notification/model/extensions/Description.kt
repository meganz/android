package mega.privacy.android.app.presentation.notification.model.extensions

import android.content.Context
import androidx.annotation.StringRes
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.extensions.spanABTextFontColour
import mega.privacy.android.domain.entity.ContactAlert
import mega.privacy.android.domain.entity.ContactChangeAccountDeletedAlert
import mega.privacy.android.domain.entity.ContactChangeBlockedYouAlert
import mega.privacy.android.domain.entity.ContactChangeContactEstablishedAlert
import mega.privacy.android.domain.entity.ContactChangeDeletedYouAlert
import mega.privacy.android.domain.entity.DeletedScheduledMeetingAlert
import mega.privacy.android.domain.entity.IncomingPendingContactCancelledAlert
import mega.privacy.android.domain.entity.IncomingPendingContactReminderAlert
import mega.privacy.android.domain.entity.IncomingPendingContactRequestAlert
import mega.privacy.android.domain.entity.NewScheduledMeetingAlert
import mega.privacy.android.domain.entity.ScheduledMeetingAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingAcceptedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingDeniedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingIgnoredAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactOutgoingAcceptedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactOutgoingDeniedAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingCancelAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingDateTimeAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingDescriptionAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingFieldsAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingTitleAlert
import mega.privacy.android.domain.entity.UserAlert

/**
 * Description
 *
 */
internal fun UserAlert.description(): (Context) -> CharSequence? =
    when (this) {
        is ContactAlert -> getDescriptionFunction()
        is ScheduledMeetingAlert -> getDescriptionFunction()
        else -> { _ -> null }
    }

/**
 * Get description id
 *
 */
@StringRes
private fun ContactAlert.getDescriptionId(): Int? = when (this) {
    is ContactChangeAccountDeletedAlert -> R.string.subtitle_account_notification_deleted
    is ContactChangeBlockedYouAlert -> R.string.subtitle_contact_notification_blocked
    is ContactChangeContactEstablishedAlert -> R.string.notification_new_contact
    is ContactChangeDeletedYouAlert -> R.string.subtitle_contact_notification_deleted
    is IncomingPendingContactCancelledAlert -> R.string.subtitle_contact_request_notification_cancelled
    is IncomingPendingContactReminderAlert -> R.string.notification_reminder_contact_request
    is IncomingPendingContactRequestAlert -> R.string.notification_new_contact_request
    is UpdatedPendingContactIncomingAcceptedAlert -> R.string.subtitle_incoming_contact_request_accepted
    is UpdatedPendingContactIncomingDeniedAlert -> R.string.subtitle_outgoing_contact_request_denied
    is UpdatedPendingContactIncomingIgnoredAlert -> R.string.subtitle_incoming_contact_request_ignored
    is UpdatedPendingContactOutgoingAcceptedAlert -> R.string.subtitle_outgoing_contact_request_accepted
    is UpdatedPendingContactOutgoingDeniedAlert -> R.string.subtitle_incoming_contact_request_denied
    else -> null
}

/**
 * Get description function
 *
 */
private fun ContactAlert.getDescriptionFunction(): (Context) -> CharSequence? =
    { context ->
        this.getDescriptionId()?.let {
            context.getFormattedStringOrDefault(it,
                this.contact.getNicknameStringOrEmail(context)).spanABTextFontColour(context)
        }
    }

/**
 * Get description function
 *
 */
private fun ScheduledMeetingAlert.getDescriptionFunction(): (Context) -> CharSequence? =
    { context ->
        when (this) {
            is NewScheduledMeetingAlert -> {
                context.getFormattedStringOrDefault(
                    R.string.notification_subtitle_scheduled_meeting_new,
                    email
                ).spanABTextFontColour(context)
            }
            is UpdatedScheduledMeetingCancelAlert, is DeletedScheduledMeetingAlert -> {
                context.getFormattedStringOrDefault(
                    R.string.notification_subtitle_scheduled_meeting_canceled,
                    email
                ).spanABTextFontColour(context)
            }
            is UpdatedScheduledMeetingTitleAlert -> {
                context.getFormattedStringOrDefault(
                    R.string.notification_subtitle_scheduled_meeting_updated_title,
                    email,
                    oldTitle,
                    title
                ).spanABTextFontColour(context)
            }
            is UpdatedScheduledMeetingDescriptionAlert -> {
                context.getFormattedStringOrDefault(
                    R.string.notification_subtitle_scheduled_meeting_updated_description,
                    email
                ).spanABTextFontColour(context)
            }
            is UpdatedScheduledMeetingDateTimeAlert -> {
                if (hasDateChanged) {
                    context.getFormattedStringOrDefault(
                        R.string.notification_subtitle_scheduled_meeting_updated_date,
                        email
                    ).spanABTextFontColour(context)
                } else {
                    context.getFormattedStringOrDefault(
                        R.string.notification_subtitle_scheduled_meeting_updated_time,
                        email
                    ).spanABTextFontColour(context)
                }
            }
            is UpdatedScheduledMeetingFieldsAlert -> {
                context.getFormattedStringOrDefault(
                    R.string.notification_subtitle_scheduled_meeting_updated_multiple,
                    email
                ).spanABTextFontColour(context)
            }
            else -> {
                null
            }
        }
    }
