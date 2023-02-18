package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.ContactChangeAccountDeletedAlert
import mega.privacy.android.domain.entity.ContactChangeBlockedYouAlert
import mega.privacy.android.domain.entity.ContactChangeContactEstablishedAlert
import mega.privacy.android.domain.entity.ContactChangeDeletedYouAlert
import mega.privacy.android.domain.entity.DeletedScheduledMeetingAlert
import mega.privacy.android.domain.entity.DeletedShareAlert
import mega.privacy.android.domain.entity.IncomingPendingContactCancelledAlert
import mega.privacy.android.domain.entity.IncomingPendingContactReminderAlert
import mega.privacy.android.domain.entity.IncomingPendingContactRequestAlert
import mega.privacy.android.domain.entity.NewScheduledMeetingAlert
import mega.privacy.android.domain.entity.NewShareAlert
import mega.privacy.android.domain.entity.NewSharedNodesAlert
import mega.privacy.android.domain.entity.PaymentFailedAlert
import mega.privacy.android.domain.entity.PaymentReminderAlert
import mega.privacy.android.domain.entity.PaymentSucceededAlert
import mega.privacy.android.domain.entity.RemovedFromShareByOwnerAlert
import mega.privacy.android.domain.entity.RemovedSharedNodesAlert
import mega.privacy.android.domain.entity.ScheduledMeetingChangeType
import mega.privacy.android.domain.entity.TakeDownAlert
import mega.privacy.android.domain.entity.TakeDownReinstatedAlert
import mega.privacy.android.domain.entity.UnknownAlert
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
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUserAlert
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Map [MegaUserAlert] to [UserAlert]
 */
typealias UserAlertMapper = @JvmSuppressWildcards suspend (@JvmSuppressWildcards MegaUserAlert, @JvmSuppressWildcards UserAlertContactProvider, @JvmSuppressWildcards UserAlertScheduledMeetingProvider, @JvmSuppressWildcards UserAlertScheduledMeetingOccurrProvider, @JvmSuppressWildcards NodeProvider) -> @JvmSuppressWildcards UserAlert


/**
 * Provide the [Contact] associated with an alert
 */
typealias UserAlertContactProvider = suspend (@JvmSuppressWildcards Long, @JvmSuppressWildcards String?) -> @JvmSuppressWildcards Contact

/**
 * Provide the Scheduled meeting associated with an alert
 */
typealias UserAlertScheduledMeetingProvider = suspend (@JvmSuppressWildcards Long, @JvmSuppressWildcards Long) -> @JvmSuppressWildcards ChatScheduledMeeting?

/**
 * Provide the Scheduled meeting occurrence associated with an alert
 */
typealias UserAlertScheduledMeetingOccurrProvider = suspend (@JvmSuppressWildcards Long) -> @JvmSuppressWildcards List<ChatScheduledMeetingOccurr>?


typealias NodeProvider = suspend (@JvmSuppressWildcards Long) -> @JvmSuppressWildcards MegaNode?

private const val CREATED_TIME_INDEX = 0L

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
    scheduledMeetingProvider: UserAlertScheduledMeetingProvider,
    scheduledMeetingOccurrProvider: UserAlertScheduledMeetingOccurrProvider,
    nodeProvider: NodeProvider,
): UserAlert = when (megaUserAlert.type) {
    MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REQUEST -> {
        IncomingPendingContactRequestAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
        )
    }
    MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_CANCELLED -> {
        IncomingPendingContactCancelledAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
        )
    }
    MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REMINDER -> {
        IncomingPendingContactReminderAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
        )
    }
    MegaUserAlert.TYPE_CONTACTCHANGE_DELETEDYOU -> {
        ContactChangeDeletedYouAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
        )
    }
    MegaUserAlert.TYPE_CONTACTCHANGE_CONTACTESTABLISHED -> {
        ContactChangeContactEstablishedAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
        )
    }
    MegaUserAlert.TYPE_CONTACTCHANGE_ACCOUNTDELETED -> {
        ContactChangeAccountDeletedAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
        )
    }
    MegaUserAlert.TYPE_CONTACTCHANGE_BLOCKEDYOU -> {
        ContactChangeBlockedYouAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
        )
    }
    MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_IGNORED -> {
        UpdatedPendingContactIncomingIgnoredAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
        )
    }
    MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_ACCEPTED -> {
        UpdatedPendingContactIncomingAcceptedAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
        )
    }
    MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_DENIED -> {
        UpdatedPendingContactIncomingDeniedAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
        )
    }
    MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_ACCEPTED -> {
        UpdatedPendingContactOutgoingAcceptedAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
        )
    }
    MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_DENIED -> {
        UpdatedPendingContactOutgoingDeniedAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
        )
    }
    MegaUserAlert.TYPE_NEWSHARE -> {
        NewShareAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
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
                createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
                isOwnChange = megaUserAlert.isOwnChange,
                nodeId = getNode(megaUserAlert, nodeProvider)?.handle,
                contact = contactProvider(megaUserAlert.userHandle, megaUserAlert.email),
            )
        } else {
            val validNodeId = megaUserAlert.nodeHandle.takeIf { it.isValid() }
            DeletedShareAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
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
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
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
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
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
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            heading = megaUserAlert.heading,
            title = megaUserAlert.title,
        )
    }
    MegaUserAlert.TYPE_PAYMENT_FAILED -> {
        PaymentFailedAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            heading = megaUserAlert.heading,
            title = megaUserAlert.title,
        )
    }
    MegaUserAlert.TYPE_PAYMENTREMINDER -> {
        PaymentReminderAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            heading = megaUserAlert.heading,
            title = megaUserAlert.title,
        )
    }
    MegaUserAlert.TYPE_TAKEDOWN -> {
        TakeDownAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
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
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            heading = megaUserAlert.heading,
            rootNodeId = getRootNodeId(megaUserAlert, nodeProvider),
            name = megaUserAlert.name,
            path = megaUserAlert.path,
        )
    }
    MegaUserAlert.TYPE_SCHEDULEDMEETING_NEW -> {
        val meeting = scheduledMeetingProvider(megaUserAlert.nodeHandle, megaUserAlert.schedId)
        if (!megaUserAlert.pcrHandle.isValid()) {
            NewScheduledMeetingAlert(
                id = megaUserAlert.id,
                seen = megaUserAlert.seen,
                createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
                isOwnChange = megaUserAlert.isOwnChange,
                chatId = megaUserAlert.nodeHandle,
                title = meeting?.title ?: megaUserAlert.title,
                email = megaUserAlert.email,
                startDate = meeting?.startDateTime,
                endDate = meeting?.endDateTime,
                isRecurring = meeting?.rules != null,
                isOccurrence = false
            )
        } else { // Updated Occurrence
            megaUserAlert.getUpdatedMeetingAlert(meeting, scheduledMeetingOccurrProvider)
        }
    }
    MegaUserAlert.TYPE_SCHEDULEDMEETING_DELETED -> {
        val meeting = scheduledMeetingProvider(megaUserAlert.nodeHandle, megaUserAlert.schedId)
        DeletedScheduledMeetingAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            chatId = megaUserAlert.nodeHandle,
            title = meeting?.title ?: megaUserAlert.title,
            email = megaUserAlert.email,
            startDate = meeting?.startDateTime,
            endDate = meeting?.endDateTime,
            isRecurring = meeting?.rules != null,
            isOccurrence = meeting?.parentSchedId.isValid()
        )
    }
    MegaUserAlert.TYPE_SCHEDULEDMEETING_UPDATED -> {
        val meeting = scheduledMeetingProvider(megaUserAlert.nodeHandle, megaUserAlert.schedId)
        megaUserAlert.getUpdatedMeetingAlert(meeting, scheduledMeetingOccurrProvider)
    }
    else -> {
        UnknownAlert(
            id = megaUserAlert.id,
            seen = megaUserAlert.seen,
            createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
            isOwnChange = megaUserAlert.isOwnChange,
            title = megaUserAlert.title
        )
    }
}

private suspend fun MegaUserAlert.getUpdatedMeetingAlert(
    meeting: ChatScheduledMeeting?,
    scheduledMeetingOccurrProvider: UserAlertScheduledMeetingOccurrProvider,
): UserAlert {
    val createdTime = getTimestamp(CREATED_TIME_INDEX)
    val isOccurrence = pcrHandle.isValid()
    if (isOccurrence) {
        scheduledMeetingOccurrProvider(nodeHandle)?.firstOrNull { occurr ->
            occurr.schedId == this.schedId
                    && occurr.parentSchedId == this.pcrHandle
                    && occurr.overrides == getNumber(0)
        }?.let { updatedOccurrence ->
            return if ((updatedOccurrence.cancelled ?: 0) > 0) {
                UpdatedScheduledMeetingCancelAlert(
                    id = id,
                    seen = seen,
                    createdTime = createdTime,
                    isOwnChange = isOwnChange,
                    chatId = nodeHandle,
                    title = meeting?.title ?: title,
                    email = email,
                    startDate = meeting?.startDateTime,
                    endDate = meeting?.endDateTime,
                    isRecurring = true,
                    isOccurrence = true,
                )
            } else {
                val dateTimeChanges = getDateTimeChanges()
                UpdatedScheduledMeetingDateTimeAlert(
                    id = id,
                    seen = seen,
                    createdTime = createdTime,
                    isOwnChange = isOwnChange,
                    chatId = nodeHandle,
                    title = meeting?.title ?: title,
                    email = email,
                    startDate = updatedStartDate?.get(1) ?: meeting?.startDateTime,
                    endDate = updatedEndDate?.get(1) ?: meeting?.endDateTime,
                    hasDateChanged = dateTimeChanges.first,
                    hasTimeChanged = dateTimeChanges.second,
                    isRecurring = true,
                    isOccurrence = true,
                )
            }
        }
    }

    val isRecurring = meeting?.rules != null
    val changes = getScheduledMeetingChanges()
    return if (changes.size == 1) {
        when (changes.first()) {
            ScheduledMeetingChangeType.Title -> {
                UpdatedScheduledMeetingTitleAlert(
                    id = id,
                    seen = seen,
                    createdTime = createdTime,
                    isOwnChange = isOwnChange,
                    chatId = nodeHandle,
                    title = updatedTitle[1],
                    email = email,
                    startDate = meeting?.startDateTime,
                    endDate = meeting?.endDateTime,
                    oldTitle = updatedTitle[0],
                    isRecurring = isRecurring,
                    isOccurrence = isOccurrence,
                )
            }
            ScheduledMeetingChangeType.Description -> {
                UpdatedScheduledMeetingDescriptionAlert(
                    id = id,
                    seen = seen,
                    createdTime = createdTime,
                    isOwnChange = isOwnChange,
                    chatId = nodeHandle,
                    title = meeting?.title ?: title,
                    email = email,
                    startDate = meeting?.startDateTime,
                    endDate = meeting?.endDateTime,
                    isRecurring = isRecurring,
                    isOccurrence = isOccurrence,
                )
            }
            ScheduledMeetingChangeType.StartDate, ScheduledMeetingChangeType.EndDate -> {
                val dateTimeChanges = getDateTimeChanges()
                UpdatedScheduledMeetingDateTimeAlert(
                    id = id,
                    seen = seen,
                    createdTime = createdTime,
                    isOwnChange = isOwnChange,
                    chatId = nodeHandle,
                    title = meeting?.title ?: title,
                    email = email,
                    startDate = updatedStartDate?.get(1) ?: meeting?.startDateTime,
                    endDate = updatedEndDate?.get(1) ?: meeting?.endDateTime,
                    hasDateChanged = dateTimeChanges.first,
                    hasTimeChanged = dateTimeChanges.second,
                    isRecurring = isRecurring,
                    isOccurrence = isOccurrence,
                )
            }
            ScheduledMeetingChangeType.Canceled -> {
                UpdatedScheduledMeetingCancelAlert(
                    id = id,
                    seen = seen,
                    createdTime = createdTime,
                    isOwnChange = isOwnChange,
                    chatId = nodeHandle,
                    title = meeting?.title ?: title,
                    email = email,
                    startDate = meeting?.startDateTime,
                    endDate = meeting?.endDateTime,
                    isRecurring = isRecurring,
                    isOccurrence = isOccurrence,
                )
            }
            else -> {
                UpdatedScheduledMeetingFieldsAlert(
                    id = id,
                    seen = seen,
                    createdTime = createdTime,
                    isOwnChange = isOwnChange,
                    chatId = nodeHandle,
                    title = meeting?.title ?: title,
                    email = email,
                    startDate = meeting?.startDateTime,
                    endDate = meeting?.endDateTime,
                    isRecurring = isRecurring,
                    isOccurrence = isOccurrence,
                )
            }
        }
    } else {
        if (changes.size == 2 && changes.containsAll(
                listOf(
                    ScheduledMeetingChangeType.StartDate,
                    ScheduledMeetingChangeType.EndDate
                )
            )
        ) {
            val dateTimeChanges = getDateTimeChanges()
            UpdatedScheduledMeetingDateTimeAlert(
                id = id,
                seen = seen,
                createdTime = createdTime,
                isOwnChange = isOwnChange,
                chatId = nodeHandle,
                title = meeting?.title ?: title,
                email = email,
                startDate = updatedStartDate?.get(1)
                    ?: meeting?.startDateTime,
                endDate = updatedEndDate?.get(1)
                    ?: meeting?.endDateTime,
                hasDateChanged = dateTimeChanges.first,
                hasTimeChanged = dateTimeChanges.second,
                isRecurring = isRecurring,
                isOccurrence = isOccurrence,
            )
        } else {
            UpdatedScheduledMeetingFieldsAlert(
                id = id,
                seen = seen,
                createdTime = createdTime,
                isOwnChange = isOwnChange,
                chatId = nodeHandle,
                title = meeting?.title ?: title,
                email = email,
                startDate = meeting?.startDateTime,
                endDate = meeting?.endDateTime,
                isRecurring = isRecurring,
                isOccurrence = isOccurrence,
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

private fun MegaUserAlert.getScheduledMeetingChanges(): List<ScheduledMeetingChangeType> =
    ScheduledMeetingChangeType.values().filter { hasSchedMeetingChanged(it.value) }

private fun Long?.isValid() = this != null && this != -1L

private fun MegaUserAlert.getDateTimeChanges(): Pair<Boolean, Boolean> {
    var hasDateChanged = false
    var hasTimeChanged = false
    val oldStartDateTime = updatedStartDate?.get(0)?.toZonedDateTime()
    val newStartDateTime = updatedStartDate?.get(1)?.toZonedDateTime()
    val oldEndDateTime = updatedEndDate?.get(0)?.toZonedDateTime()
    val newEndDateTime = updatedEndDate?.get(1)?.toZonedDateTime()

    if (oldStartDateTime != null && newStartDateTime != null) {
        if (oldStartDateTime.toLocalDate() != newStartDateTime.toLocalDate())
            hasDateChanged = true
        if (oldStartDateTime.toLocalTime() != newStartDateTime.toLocalTime())
            hasTimeChanged = true
    }
    if (oldEndDateTime != null && newEndDateTime != null) {
        if (oldEndDateTime.toLocalDate() != newEndDateTime.toLocalDate())
            hasDateChanged = true
        if (oldEndDateTime.toLocalTime() != newEndDateTime.toLocalTime())
            hasTimeChanged = true
    }

    return hasDateChanged to hasTimeChanged
}

private fun Long.toZonedDateTime(): ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneOffset.UTC)
