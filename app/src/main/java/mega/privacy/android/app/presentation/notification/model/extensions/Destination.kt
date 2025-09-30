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
import mega.privacy.android.domain.entity.UserAlertDestination
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.android.navigation.destination.MyAccountNavKey
import mega.privacy.android.navigation.destination.RubbishBinNavKey

/**
 * Notification Destination
 *
 */
internal fun UserAlert.destination(): NavKey? {
    return when (this) {
        is PaymentFailedAlert -> MyAccountNavKey
        is PaymentReminderAlert -> MyAccountNavKey
        is PaymentSucceededAlert -> MyAccountNavKey
        is TakeDownAlert -> when (destination) {
            UserAlertDestination.CloudDrive -> rootNodeId?.let { CloudDriveNavKey(it) }
            UserAlertDestination.RubbishBin -> rootNodeId?.let { RubbishBinNavKey(it) }
            UserAlertDestination.IncomingShares -> null //TODO handle IncomingShares JIRA AND-21337
            else -> null
        }

        is TakeDownReinstatedAlert -> when (destination) {
            UserAlertDestination.CloudDrive -> rootNodeId?.let { CloudDriveNavKey(it) }
            UserAlertDestination.RubbishBin -> rootNodeId?.let { RubbishBinNavKey(it) }
            UserAlertDestination.IncomingShares -> null // TODO handle IncomingShares AND-21337
            else -> null
        }

        is ContactChangeAccountDeletedAlert -> null
        is ContactChangeBlockedYouAlert -> null
        is ContactChangeDeletedYouAlert -> null
        is IncomingPendingContactCancelledAlert -> null
        is UpdatedPendingContactIncomingDeniedAlert -> null
        is UpdatedPendingContactIncomingIgnoredAlert -> null
        is UpdatedPendingContactOutgoingDeniedAlert -> null
        is IncomingShareAlert -> null //TODO handle IncomingShareAlert AND-21337

        is ContactAlert ->
            if (contact.isVisible) {
                contact.email?.let { ContactInfoNavKey(it) }
            } else if (contact.hasPendingRequest) {
                ContactsNavKey(ContactsNavKey.NavType.ReceivedRequests)
            } else null

        is ScheduledMeetingAlert -> ChatNavKey(chatId, Constants.ACTION_CHAT_SHOW_MESSAGES)

        else -> null
    }
}