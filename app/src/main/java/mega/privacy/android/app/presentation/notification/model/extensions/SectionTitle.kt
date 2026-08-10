package mega.privacy.android.app.presentation.notification.model.extensions

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.ContactAlert
import mega.privacy.android.domain.entity.CustomAlert
import mega.privacy.android.domain.entity.IncomingShareAlert
import mega.privacy.android.domain.entity.PaymentFailedAlert
import mega.privacy.android.domain.entity.PaymentReminderAlert
import mega.privacy.android.domain.entity.PaymentSucceededAlert
import mega.privacy.android.domain.entity.ScheduledMeetingAlert
import mega.privacy.android.domain.entity.UserAlert
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Section title
 *
 */
internal fun UserAlert.sectionTitle(): (Context) -> String = when (this) {
    is ContactAlert -> { context ->
        context.getString(sharedR.string.general_section_contacts)
    }

    is IncomingShareAlert -> { context ->
        context.getString(sharedR.string.general_title_incoming_shares)
    }

    is ScheduledMeetingAlert -> { context ->
        context.getString(R.string.chat_tab_meetings_title)
    }

    is PaymentSucceededAlert, is PaymentFailedAlert -> { context ->
        context.getString(sharedR.string.notifications_payment_info_section)
    }

    is PaymentReminderAlert -> { context ->
        val nowInSeconds = System.currentTimeMillis() / 1000
        val sectionRes = if (endTimestamp < nowInSeconds) {
            sharedR.string.notifications_payment_reminder_expired_section
        } else {
            sharedR.string.notifications_payment_reminder_section
        }
        context.getString(sectionRes)
    }

    is CustomAlert -> { _ ->
        heading ?: ""
    }

    else -> { _ -> "" }
}