package mega.privacy.android.app.presentation.notification.model.extensions

import mega.privacy.android.app.presentation.notification.model.NotificationNavigationHandler
import mega.privacy.android.domain.entity.ContactAlert
import mega.privacy.android.domain.entity.ContactChangeAccountDeletedAlert
import mega.privacy.android.domain.entity.ContactChangeBlockedYouAlert
import mega.privacy.android.domain.entity.ContactChangeDeletedYouAlert
import mega.privacy.android.domain.entity.IncomingPendingContactCancelledAlert
import mega.privacy.android.domain.entity.IncomingShareAlert
import mega.privacy.android.domain.entity.NewSharedNodesAlert
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
import timber.log.Timber

/**
 * On click
 *
 */
internal fun UserAlert.onClick(): (NotificationNavigationHandler) -> Unit {
    return when (this) {
        is PaymentFailedAlert -> { handler ->
            handler.navigateToMyAccount()
        }
        is PaymentReminderAlert -> { handler ->
            handler.navigateToMyAccount()
        }
        is PaymentSucceededAlert -> { handler ->
            handler.navigateToMyAccount()
        }
        is TakeDownAlert -> { handler ->
            rootNodeId?.let { handler.navigateToSharedNode(it, null) }
        }
        is TakeDownReinstatedAlert -> { handler ->
            rootNodeId?.let { handler.navigateToSharedNode(it, null) }
        }
        is ContactChangeAccountDeletedAlert -> { _ ->
            Timber.d("Do not navigate: ${this.javaClass.name}")
        }
        is ContactChangeBlockedYouAlert -> { _ ->
            Timber.d("Do not navigate: ${this.javaClass.name}")
        }
        is ContactChangeDeletedYouAlert -> { _ ->
            Timber.d("Do not navigate: ${this.javaClass.name}")
        }
        is IncomingPendingContactCancelledAlert -> { _ ->
            Timber.d("Do not navigate: ${this.javaClass.name}")
        }
        is UpdatedPendingContactIncomingDeniedAlert -> { _ ->
            Timber.d("Do not navigate: ${this.javaClass.name}")
        }
        is UpdatedPendingContactIncomingIgnoredAlert -> { _ ->
            Timber.d("Do not navigate: ${this.javaClass.name}")
        }
        is UpdatedPendingContactOutgoingDeniedAlert -> { _ ->
            Timber.d("Do not navigate: ${this.javaClass.name}")
        }
        is IncomingShareAlert -> { handler ->
            nodeId?.let {
                handler.navigateToSharedNode(
                    nodeId = it,
                    childNodes = if (this is NewSharedNodesAlert) childNodes.toLongArray() else null
                )
            }
        }
        is ContactAlert -> { handler ->
            if (contact.isVisible) {
                contact.email?.let { handler.navigateToContactInfo(it) }
            } else if (contact.hasPendingRequest) {
                handler.navigateToContactRequests()
            } else {
                Timber.d("Do not navigate: ${this.javaClass.name}")
            }
        }
        is ScheduledMeetingAlert -> { handler ->
            handler.moveToChatSection(chatId)
        }
        else -> { _ ->
            Timber.d("Do not navigate: ${this.javaClass.name}")
        }
    }
}