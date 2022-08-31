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
import mega.privacy.android.domain.entity.RemovedFromShareByOwnerAlert
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
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUserAlert

/**
 * Map [MegaUserAlert] to [UserAlert]
 */
typealias UserAlertMapper = @JvmSuppressWildcards suspend (@JvmSuppressWildcards MegaUserAlert, @JvmSuppressWildcards UserAlertContactProvider, @JvmSuppressWildcards NodeProvider) -> @JvmSuppressWildcards UserAlert


/**
 * Provide the [Contact] associated with an alert
 */
typealias UserAlertContactProvider = suspend (@JvmSuppressWildcards Long, @JvmSuppressWildcards String?) -> @JvmSuppressWildcards Contact


typealias NodeProvider = suspend (@JvmSuppressWildcards Long) -> @JvmSuppressWildcards MegaNode?

/**
 * Map [MegaUserAlert] to [UserAlert]
 *
 * @param megaUserAlert
 * @param contactProvider
 * @return correct [UserAlert]
 */
internal suspend fun toUserAlert(
    megaUserAlert: MegaUserAlert,
    contactProvider: UserAlertContactProvider,
    nodeProvider: NodeProvider,
): UserAlert {
    val createdTimeIndex: Long = 0

    return when (megaUserAlert.type) {
        MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REQUEST -> {
            IncomingPendingContactRequestAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
            )
        }
        MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_CANCELLED -> {
            IncomingPendingContactCancelledAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
            )
        }
        MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REMINDER -> {
            IncomingPendingContactReminderAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
            )
        }
        MegaUserAlert.TYPE_CONTACTCHANGE_DELETEDYOU -> {
            ContactChangeDeletedYouAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
            )
        }
        MegaUserAlert.TYPE_CONTACTCHANGE_CONTACTESTABLISHED -> {
            ContactChangeContactEstablishedAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
            )
        }
        MegaUserAlert.TYPE_CONTACTCHANGE_ACCOUNTDELETED -> {
            ContactChangeAccountDeletedAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
            )
        }
        MegaUserAlert.TYPE_CONTACTCHANGE_BLOCKEDYOU -> {
            ContactChangeBlockedYouAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
            )
        }
        MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_IGNORED -> {
            UpdatedPendingContactIncomingIgnoredAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
            )
        }
        MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_ACCEPTED -> {
            UpdatedPendingContactIncomingAcceptedAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
            )
        }
        MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_DENIED -> {
            UpdatedPendingContactIncomingDeniedAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
            )
        }
        MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_ACCEPTED -> {
            UpdatedPendingContactOutgoingAcceptedAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
            )
        }
        MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_DENIED -> {
            UpdatedPendingContactOutgoingDeniedAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
            )
        }
        MegaUserAlert.TYPE_NEWSHARE -> {
            NewShareAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
                nodeId = getNode(megaUserAlert, nodeProvider)?.handle,
            )
        }
        MegaUserAlert.TYPE_DELETEDSHARE -> {
            val deletionReasonIndex: Long = 0
            val removedByOwner: Long = 1
            if (megaUserAlert.getNumber(
                    deletionReasonIndex
                ) == removedByOwner
            ) {
                RemovedFromShareByOwnerAlert(
                    id = megaUserAlert.id,
                    seen = megaUserAlert.seen,
                    createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                    isOwnChange = megaUserAlert.isOwnChange,
                    nodeId = getNode(megaUserAlert, nodeProvider)?.handle,
                    contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
                )
            } else {
                val validNodeId = megaUserAlert.nodeHandle.takeIf { it.isValid() }
                DeletedShareAlert(
                    id = megaUserAlert.id,
                    seen = megaUserAlert.seen,
                    createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                    isOwnChange = megaUserAlert.isOwnChange,
                    nodeId = validNodeId,
                    nodeName = validNodeId?.let { nodeProvider(it) }?.name,
                    contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
                )
            }


        }
        MegaUserAlert.TYPE_NEWSHAREDNODES -> {
            val folderIndex: Long = 0
            val fileIndex: Long = 1
            NewSharedNodesAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                nodeId = getNode(megaUserAlert, nodeProvider)?.handle,
                folderCount = megaUserAlert.getNumber(folderIndex).toInt(),
                fileCount = megaUserAlert.getNumber(fileIndex).toInt(),
                childNodes = getChildNodes(megaUserAlert),
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
            )
        }
        MegaUserAlert.TYPE_REMOVEDSHAREDNODES -> {
            val itemCountIndex: Long = 0
            RemovedSharedNodesAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(createdTimeIndex),
                isOwnChange = megaUserAlert.isOwnChange,
                nodeId = getNode(megaUserAlert, nodeProvider)?.handle,
                itemCount = megaUserAlert.getNumber(itemCountIndex).toInt(),
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
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
                heading = megaUserAlert.heading,
                rootNodeId = getRootNodeId(megaUserAlert, nodeProvider),
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
                heading = megaUserAlert.heading,
                rootNodeId = getRootNodeId(megaUserAlert, nodeProvider),
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
                title = megaUserAlert.title
            )
        }
    }
}

private suspend fun getNode(
    megaUserAlert: MegaUserAlert,
    nodeProvider: NodeProvider,
) = megaUserAlert.nodeHandle
    .takeIf { it.isValid() }
    ?.let { nodeProvider(it) }

private suspend fun getRootNodeId(
    megaUserAlert: MegaUserAlert,
    nodeProvider: NodeProvider,
) = getNode(megaUserAlert, nodeProvider)
    ?.let {
        if (it.isFile) it.parentHandle else it.handle
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
