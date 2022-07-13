package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.ContactChangeAccountDeletedAlert
import mega.privacy.android.domain.entity.ContactChangeBlockedYouAlert
import mega.privacy.android.domain.entity.ContactChangeContactEstablishedAlert
import mega.privacy.android.domain.entity.ContactChangeDeletedYouAlert
import mega.privacy.android.domain.entity.DeletedShareAlert
import mega.privacy.android.domain.entity.IncomingPendingContactCancelledAlert
import mega.privacy.android.domain.entity.IncomingPendingContactReminderAlert
import mega.privacy.android.domain.entity.IncomingPendingContactRequestAlert
import mega.privacy.android.domain.entity.NewShareAlert
import mega.privacy.android.domain.entity.NewSharedNodesAlert
import mega.privacy.android.domain.entity.PaymentFailedAlert
import mega.privacy.android.domain.entity.PaymentReminderAlert
import mega.privacy.android.domain.entity.PaymentSucceededAlert
import mega.privacy.android.domain.entity.RemovedSharedNodesAlert
import mega.privacy.android.domain.entity.TakeDownAlert
import mega.privacy.android.domain.entity.TakeDownReinstatedAlert
import mega.privacy.android.domain.entity.UnknownAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingAcceptedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingDeniedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingIgnoredAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactOutgoingAcceptedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactOutgoingDeniedAlert
import mega.privacy.android.domain.entity.UserAlert
import nz.mega.sdk.MegaUserAlert

/**
 * Map [MegaUserAlert] to [UserAlert]
 */
typealias UserAlertMapper = @JvmSuppressWildcards suspend (@JvmSuppressWildcards MegaUserAlert, @JvmSuppressWildcards UserAlertEmailProvider, @JvmSuppressWildcards UserAlertContactProvider) -> @JvmSuppressWildcards UserAlert

/**
 * Provide the email address associated with a UserAlert
 */
typealias UserAlertEmailProvider = suspend (@JvmSuppressWildcards Long) -> @JvmSuppressWildcards String?

/**
 * Provide the [Contact] associated with an email address
 */
typealias UserAlertContactProvider = suspend (@JvmSuppressWildcards String) -> @JvmSuppressWildcards Contact?


/**
 * Map [MegaUserAlert] to [UserAlert]
 *
 * @param megaUserAlert
 * @param emailProvider
 * @param contactProvider
 * @return correct [UserAlert]
 */
internal suspend fun toUserAlert(
    megaUserAlert: MegaUserAlert,
    emailProvider: UserAlertEmailProvider,
    contactProvider: UserAlertContactProvider,
): UserAlert {
    val createdTimeIndex: Long = 0

    return when (megaUserAlert.type) {
        MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REQUEST -> {
            val email = megaUserAlert.email ?: emailProvider(megaUserAlert.userHandle)
            IncomingPendingContactRequestAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                userId = megaUserAlert.userHandle,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                email = email,
                contact = email?.let { contactProvider(it) },
            )
        }
        MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_CANCELLED -> {
            val email = megaUserAlert.email ?: emailProvider(megaUserAlert.userHandle)
            IncomingPendingContactCancelledAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                userId = megaUserAlert.userHandle,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                email = email,
                contact = email?.let { contactProvider(it) },
            )
        }
        MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REMINDER -> {
            val email = megaUserAlert.email ?: emailProvider(megaUserAlert.userHandle)
            IncomingPendingContactReminderAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                userId = megaUserAlert.userHandle,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                email = email,
                contact = email?.let { contactProvider(it) },
            )
        }
        MegaUserAlert.TYPE_CONTACTCHANGE_DELETEDYOU -> {
            val email = megaUserAlert.email ?: emailProvider(megaUserAlert.userHandle)
            ContactChangeDeletedYouAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                userId = megaUserAlert.userHandle,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                email = email,
                contact = email?.let { contactProvider(it) },
            )
        }
        MegaUserAlert.TYPE_CONTACTCHANGE_CONTACTESTABLISHED -> {
            val email = megaUserAlert.email ?: emailProvider(megaUserAlert.userHandle)
            ContactChangeContactEstablishedAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                userId = megaUserAlert.userHandle,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                email = email,
                contact = email?.let { contactProvider(it) },
            )
        }
        MegaUserAlert.TYPE_CONTACTCHANGE_ACCOUNTDELETED -> {
            val email = megaUserAlert.email ?: emailProvider(megaUserAlert.userHandle)
            ContactChangeAccountDeletedAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                userId = megaUserAlert.userHandle,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                email = email,
                contact = email?.let { contactProvider(it) },
            )
        }
        MegaUserAlert.TYPE_CONTACTCHANGE_BLOCKEDYOU -> {
            val email = megaUserAlert.email ?: emailProvider(megaUserAlert.userHandle)
            ContactChangeBlockedYouAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                userId = megaUserAlert.userHandle,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                email = email,
                contact = email?.let { contactProvider(it) },
            )
        }
        MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_IGNORED -> {
            val email = megaUserAlert.email ?: emailProvider(megaUserAlert.userHandle)
            UpdatedPendingContactIncomingIgnoredAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                userId = megaUserAlert.userHandle,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                email = email,
                contact = email?.let { contactProvider(it) },
            )
        }
        MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_ACCEPTED -> {
            val email = megaUserAlert.email ?: emailProvider(megaUserAlert.userHandle)
            UpdatedPendingContactIncomingAcceptedAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                userId = megaUserAlert.userHandle,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                email = email,
                contact = email?.let { contactProvider(it) },
            )
        }
        MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_DENIED -> {
            val email = megaUserAlert.email ?: emailProvider(megaUserAlert.userHandle)
            UpdatedPendingContactIncomingDeniedAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                userId = megaUserAlert.userHandle,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                email = email,
                contact = email?.let { contactProvider(it) },
            )
        }
        MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_ACCEPTED -> {
            val email = megaUserAlert.email ?: emailProvider(megaUserAlert.userHandle)
            UpdatedPendingContactOutgoingAcceptedAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                userId = megaUserAlert.userHandle,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                email = email,
                contact = email?.let { contactProvider(it) },
            )
        }
        MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_DENIED -> {
            val email = megaUserAlert.email ?: emailProvider(megaUserAlert.userHandle)
            UpdatedPendingContactOutgoingDeniedAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                userId = megaUserAlert.userHandle,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                email = email,
                contact = email?.let { contactProvider(it) },
            )
        }
        MegaUserAlert.TYPE_NEWSHARE -> {
            NewShareAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                email = megaUserAlert.email ?: emailProvider(megaUserAlert.userHandle),
                nodeId = megaUserAlert.nodeHandle.takeIf { it.isValid() },
            )
        }
        MegaUserAlert.TYPE_DELETEDSHARE -> {
            val deletionReasonIndex: Long = 0
            val removedByOwner: Long = 1
            DeletedShareAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                nodeId = megaUserAlert.nodeHandle.takeUnless {
                    !it.isValid() ||
                            megaUserAlert.getNumber(
                                deletionReasonIndex
                            ) == removedByOwner
                }
            )
        }
        MegaUserAlert.TYPE_NEWSHAREDNODES -> {
            val folderIndex: Long = 0
            val fileIndex: Long = 1
            NewSharedNodesAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                nodeId = megaUserAlert.nodeHandle.takeIf { it.isValid() },
                folderCount = megaUserAlert.getNumber(folderIndex),
                fileCount = megaUserAlert.getNumber(fileIndex),
                childNodes = getChildNodes(megaUserAlert)
            )
        }
        MegaUserAlert.TYPE_REMOVEDSHAREDNODES -> {
            val itemCountIndex: Long = 0
            RemovedSharedNodesAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                nodeId = megaUserAlert.nodeHandle.takeIf { it.isValid() },
                itemCount = megaUserAlert.getNumber(itemCountIndex),
            )
        }
        MegaUserAlert.TYPE_PAYMENT_SUCCEEDED -> {
            PaymentSucceededAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                heading = megaUserAlert.heading,
                title = megaUserAlert.title,
            )
        }
        MegaUserAlert.TYPE_PAYMENT_FAILED -> {
            PaymentFailedAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                heading = megaUserAlert.heading,
                title = megaUserAlert.title,
            )
        }
        MegaUserAlert.TYPE_PAYMENTREMINDER -> {
            PaymentReminderAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                heading = megaUserAlert.heading,
                title = megaUserAlert.title,
            )
        }
        MegaUserAlert.TYPE_TAKEDOWN -> {
            TakeDownAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                nodeId = megaUserAlert.nodeHandle.takeIf { it.isValid() },
                name = megaUserAlert.name,
                path = megaUserAlert.path,
            )
        }
        MegaUserAlert.TYPE_TAKEDOWN_REINSTATED -> {
            TakeDownReinstatedAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                nodeId = megaUserAlert.nodeHandle.takeIf { it.isValid() },
                name = megaUserAlert.name,
                path = megaUserAlert.path,
            )
        }
        else -> {
            UnknownAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
            )
        }
    }
}

private fun getChildNodes(megaUserAlert: MegaUserAlert) =
    mutableListOf<Long>().apply {
        var i = 0L
        var nodeId = megaUserAlert.getHandle(i)
        while (nodeId.isValid()) {
            add(nodeId)
            nodeId = megaUserAlert.getHandle(++i)
        }
    }

private fun Long.isValid() = this != -1L
