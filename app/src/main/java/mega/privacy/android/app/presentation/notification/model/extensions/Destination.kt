package mega.privacy.android.app.presentation.notification.model.extensions

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ContactAlert
import mega.privacy.android.domain.entity.ContactChangeAccountDeletedAlert
import mega.privacy.android.domain.entity.ContactChangeBlockedYouAlert
import mega.privacy.android.domain.entity.ContactChangeDeletedYouAlert
import mega.privacy.android.domain.entity.IncomingPendingContactCancelledAlert
import mega.privacy.android.domain.entity.IncomingShareAlert
import mega.privacy.android.domain.entity.PaymentFailedAlert
import mega.privacy.android.domain.entity.PaymentReminderAlert
import mega.privacy.android.domain.entity.PaymentSucceededAlert
import mega.privacy.android.domain.entity.ScheduledMeetingAlert
import mega.privacy.android.domain.entity.TakeDownAlert
import mega.privacy.android.domain.entity.TakeDownReinstatedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingDeniedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingIgnoredAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactOutgoingDeniedAlert
import mega.privacy.android.domain.entity.UserAlert
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.android.navigation.destination.MyAccountNavKey

/**
 * Notification Destination
 *
 */
internal fun UserAlert.destination(): NavKey? {
    return when (this) {
        is PaymentFailedAlert -> MyAccountNavKey
        is PaymentReminderAlert -> MyAccountNavKey
        is PaymentSucceededAlert -> MyAccountNavKey
        is TakeDownAlert -> null //TODO handle TakeDownAlert

        is TakeDownReinstatedAlert -> null //TODO handle TakeDownReinstatedAlert

        is ContactChangeAccountDeletedAlert -> null
        is ContactChangeBlockedYouAlert -> null
        is ContactChangeDeletedYouAlert -> null
        is IncomingPendingContactCancelledAlert -> null
        is UpdatedPendingContactIncomingDeniedAlert -> null
        is UpdatedPendingContactIncomingIgnoredAlert -> null
        is UpdatedPendingContactOutgoingDeniedAlert -> null
        is IncomingShareAlert -> null //TODO handle IncomingShareAlert

        is ContactAlert ->
            if (contact.isVisible) {
                contact.email?.let { ContactInfoNavKey(it) }
            } else if (contact.hasPendingRequest) {
                ContactsNavKey
            } else null

        is ScheduledMeetingAlert -> ChatNavKey(chatId, Constants.ACTION_CHAT_SHOW_MESSAGES)

        else -> null
    }
}