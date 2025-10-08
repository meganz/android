package mega.privacy.android.data.mapper

import mega.privacy.android.data.mapper.useralert.UserAlertChangesMapper
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
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingRulesAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingTimezoneAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingTitleAlert
import mega.privacy.android.domain.entity.UserAlert
import mega.privacy.android.domain.entity.UserAlertDestination
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.useralert.UserAlertChange
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUserAlert
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.inject.Inject

private const val CREATED_TIME_INDEX = 0L

internal class UserAlertMapper @Inject constructor() {
    suspend operator fun invoke(
        megaUserAlert: MegaUserAlert,
        contactProvider: suspend (Long, String?) -> Contact,
        scheduledMeetingProvider: suspend (Long, Long) -> ChatScheduledMeeting?,
        scheduledMeetingOccurrProvider: suspend (Long) -> List<ChatScheduledMeetingOccurr>?,
        nodeProvider: suspend (Long) -> MegaNode?,
        rootParentNodeProvider: suspend (Long) -> MegaNode?,
        rubbishNodeProvider: suspend () -> MegaNode?,
        rootNodeProvider: suspend () -> MegaNode?,
    ): UserAlert = toUserAlert(
        megaUserAlert,
        contactProvider,
        scheduledMeetingProvider,
        scheduledMeetingOccurrProvider,
        nodeProvider,
        rootParentNodeProvider,
        rubbishNodeProvider,
        rootNodeProvider
    )
}

/**
 * Map [MegaUserAlert] to [UserAlert]
 *
 * @param megaUserAlert
 * @param contactProvider
 * @return correct [UserAlert]
 */
internal suspend fun toUserAlert(
    megaUserAlert: MegaUserAlert,
    contactProvider: suspend (Long, String?) -> Contact,
    scheduledMeetingProvider: suspend (Long, Long) -> ChatScheduledMeeting?,
    scheduledMeetingOccurrProvider: suspend (Long) -> List<ChatScheduledMeetingOccurr>?,
    nodeProvider: suspend (Long) -> MegaNode?,
    rootParentNodeProvider: suspend (Long) -> MegaNode?,
    rubbishNodeProvider: suspend () -> MegaNode?,
    rootNodeProvider: suspend () -> MegaNode?,
): UserAlert {
    return when (megaUserAlert.type) {
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
                val node = getNode(megaUserAlert, nodeProvider)
                DeletedShareAlert(
                    id = megaUserAlert.id,
                    seen = megaUserAlert.seen,
                    createdTime = megaUserAlert.getTimestamp(CREATED_TIME_INDEX),
                    isOwnChange = megaUserAlert.isOwnChange,
                    nodeId = node?.handle,
                    nodeName = node?.name,
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
                destination = getDestination(
                    megaUserAlert,
                    nodeProvider,
                    rootParentNodeProvider,
                    rubbishNodeProvider,
                    rootNodeProvider
                )
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
                destination = getDestination(
                    megaUserAlert,
                    nodeProvider,
                    rootParentNodeProvider,
                    rubbishNodeProvider,
                    rootNodeProvider
                )
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
                    isOccurrence = false,
                    scheduledMeeting = meeting,
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
                isOccurrence = meeting?.parentSchedId.isValid(),
                scheduledMeeting = meeting,
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
}

private suspend fun getDestination(
    megaUserAlert: MegaUserAlert,
    nodeProvider: suspend (Long) -> MegaNode?,
    rootParentNodeProvider: suspend (Long) -> MegaNode?,
    rubbishNodeProvider: suspend () -> MegaNode?,
    rootNodeProvider: suspend () -> MegaNode?,
): UserAlertDestination? = getRootNodeId(megaUserAlert, nodeProvider)?.let {
    rootParentNodeProvider(it)?.let { rootParent ->
        val rootNode = rootNodeProvider()
        val rubbishNode = rubbishNodeProvider()
        when (rootParent.handle) {
            rootNode?.handle -> {
                UserAlertDestination.CloudDrive
            }

            rubbishNode?.handle -> {
                UserAlertDestination.RubbishBin
            }

            else -> UserAlertDestination.IncomingShares
        }
    }
}


private suspend fun MegaUserAlert.getUpdatedMeetingAlert(
    meeting: ChatScheduledMeeting?,
    scheduledMeetingOccurrProvider: suspend (Long) -> List<ChatScheduledMeetingOccurr>?,
): UserAlert {

    val createdTime = getTimestamp(CREATED_TIME_INDEX)
    val isRecurring = meeting?.rules != null
    val isOccurrence = pcrHandle.isValid()
    if (isOccurrence) {
        scheduledMeetingOccurrProvider(nodeHandle)?.firstOrNull { occurr ->
            occurr.schedId == this.schedId
                    && occurr.parentSchedId == this.pcrHandle
                    && occurr.overrides == getNumber(0)
        }?.let { updatedOccurrence ->
            return if (updatedOccurrence.isCancelled) {
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
                    occurrenceChanged = updatedOccurrence,
                    scheduledMeeting = meeting,
                )
            } else {
                val dateTimeChanges = getDateTimeChanges(updatedOccurrence)
                val hasDayOfWeekChange = getDateOfWeekChanges()
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
                    hasDayOfWeekChanged = hasDayOfWeekChange,
                    isRecurring = true,
                    isOccurrence = true,
                    scheduledMeeting = meeting,
                )
            }
        }
    }

    if (meeting?.isCanceled == true) {
        return UpdatedScheduledMeetingCancelAlert(
            id = id,
            seen = seen,
            createdTime = createdTime,
            isOwnChange = isOwnChange,
            chatId = nodeHandle,
            title = meeting.title ?: title,
            email = email,
            startDate = meeting.startDateTime,
            endDate = meeting.endDateTime,
            isRecurring = isRecurring,
            isOccurrence = isOccurrence,
            occurrenceChanged = null,
            scheduledMeeting = meeting,
        )
    }

    val changes = getScheduledMeetingChanges()

    return if (changes.size == 1) {
        when (changes.first()) {
            UserAlertChange.Title -> {
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
                    scheduledMeeting = meeting?.takeIf { !isRecurring },
                )
            }

            UserAlertChange.Description -> {
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
                    scheduledMeeting = meeting,
                )
            }

            UserAlertChange.Canceled -> {
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
                    occurrenceChanged = null,
                    scheduledMeeting = meeting,
                )
            }

            UserAlertChange.Timezone -> {
                UpdatedScheduledMeetingTimezoneAlert(
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
                    timezone = meeting?.timezone ?: updatedTimeZone?.get(1),
                    scheduledMeeting = meeting,
                )
            }

            UserAlertChange.StartDate, UserAlertChange.EndDate -> {
                val dateTimeChanges = getDateTimeChanges()
                val hasDayOfWeekChange = getDateOfWeekChanges()
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
                    hasDayOfWeekChanged = hasDayOfWeekChange,
                    isRecurring = isRecurring,
                    isOccurrence = isOccurrence,
                    scheduledMeeting = meeting,
                )
            }

            UserAlertChange.Rules -> {
                val hasDayOfWeekChange = getDateOfWeekChanges()
                val dateTimeChanges = getDateTimeChanges()
                UpdatedScheduledMeetingRulesAlert(
                    id = id,
                    seen = seen,
                    createdTime = createdTime,
                    isOwnChange = isOwnChange,
                    chatId = nodeHandle,
                    title = meeting?.title ?: title,
                    email = email,
                    startDate = meeting?.startDateTime,
                    endDate = meeting?.endDateTime,
                    hasDateChanged = dateTimeChanges.first,
                    hasDayOfWeekChanged = hasDayOfWeekChange,
                    isRecurring = isRecurring,
                    isOccurrence = isOccurrence,
                    scheduledMeeting = meeting,
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
                    scheduledMeeting = meeting,
                )
            }
        }
    } else {
        if (changes.size == 2 && changes.containsAll(
                listOf(
                    UserAlertChange.StartDate,
                    UserAlertChange.EndDate
                )
            )
        ) {
            val dateTimeChanges = getDateTimeChanges()
            val hasDayOfWeekChange = getDateOfWeekChanges()
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
                hasDayOfWeekChanged = hasDayOfWeekChange,
                isRecurring = isRecurring,
                isOccurrence = isOccurrence,
                scheduledMeeting = meeting,
            )
        } else if (changes.contains(UserAlertChange.Rules).not()) {
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
                scheduledMeeting = meeting,
            )
        } else {
            val hasDayOfWeekChange = getDateOfWeekChanges()
            val dateTimeChanges = getDateTimeChanges()
            UpdatedScheduledMeetingRulesAlert(
                id = id,
                seen = seen,
                createdTime = createdTime,
                isOwnChange = isOwnChange,
                chatId = nodeHandle,
                title = meeting?.title ?: title,
                email = email,
                startDate = meeting?.startDateTime,
                endDate = meeting?.endDateTime,
                hasDateChanged = dateTimeChanges.first,
                hasDayOfWeekChanged = hasDayOfWeekChange,
                isRecurring = isRecurring,
                isOccurrence = isOccurrence,
                scheduledMeeting = meeting,
            )
        }
    }
}

private suspend fun getNode(
    megaUserAlert: MegaUserAlert,
    nodeProvider: suspend (Long) -> MegaNode?,
) = megaUserAlert.nodeHandle
    .takeIf { it.isValid() }
    ?.let { nodeProvider(it) }

private suspend fun getRootNodeId(
    megaUserAlert: MegaUserAlert,
    nodeProvider: suspend (Long) -> MegaNode?,
) = getNode(megaUserAlert, nodeProvider)
    ?.let {
        if (it.isFile) it.parentHandle else it.handle
    }

private fun MegaUserAlert.getScheduledMeetingChanges(): List<UserAlertChange> =
    UserAlertChange.entries.filter { alertChange ->
        hasSchedMeetingChanged(UserAlertChangesMapper.userAlertChanges[alertChange] as Long)
    }


private fun MegaUserAlert.getDateTimeChanges(
    occurrence: ChatScheduledMeetingOccurr? = null,
): Pair<Boolean, Boolean> {
    var hasDateChanged = false
    var hasTimeChanged = false

    val oldStartDateTime = updatedStartDate?.get(0)?.toZonedDateTime()
        ?: getNumber(0).toZonedDateTime()
    val newStartDateTime = updatedStartDate?.get(1)?.toZonedDateTime()
        ?: occurrence?.startDateTime?.toZonedDateTime()
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

private fun MegaUserAlert.getDateOfWeekChanges(
    occurrence: ChatScheduledMeetingOccurr? = null,
): Boolean {
    var hasDayOfWeekChanged = false

    val oldStartDateTime = updatedStartDate?.get(0)?.toZonedDateTime()
        ?: getNumber(0).toZonedDateTime()
    val oldStartDay = oldStartDateTime?.dayOfWeek

    val newStartDateTime = updatedStartDate?.get(1)?.toZonedDateTime()
        ?: occurrence?.startDateTime?.toZonedDateTime()
    val newStartDay = newStartDateTime?.dayOfWeek

    if (oldStartDay != null && newStartDay != null) {
        if (oldStartDay != newStartDay)
            hasDayOfWeekChanged = true
    }

    return hasDayOfWeekChanged
}

private fun Long?.isValid() = this != null && this != -1L

private fun Long.toZonedDateTime(): ZonedDateTime? =
    takeIf(Long::isValid)?.let { timeStamp ->
        ZonedDateTime.ofInstant(Instant.ofEpochSecond(timeStamp), ZoneOffset.UTC)
    }
