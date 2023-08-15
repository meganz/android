package mega.privacy.android.app.presentation.notification.model.extensions

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.Util
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
import mega.privacy.android.domain.entity.ScheduledMeetingAlert
import mega.privacy.android.domain.entity.TakeDownAlert
import mega.privacy.android.domain.entity.TakeDownReinstatedAlert
import mega.privacy.android.domain.entity.UnknownAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingAcceptedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingDeniedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingIgnoredAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactOutgoingAcceptedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactOutgoingDeniedAlert
import mega.privacy.android.domain.entity.UserAlert

internal fun UserAlert.title(): (Context) -> String = when (this) {
    is IncomingPendingContactRequestAlert -> { context ->
        context.getString(R.string.title_contact_request_notification)
    }

    is IncomingPendingContactReminderAlert -> { context ->
        context.getString(R.string.title_contact_request_notification)
    }

    is IncomingPendingContactCancelledAlert -> { context ->
        context.getString(R.string.title_contact_request_notification_cancelled)
    }

    is ContactChangeContactEstablishedAlert -> { context ->
        context.getString(R.string.title_acceptance_contact_request_notification)
    }

    is ContactChangeAccountDeletedAlert -> { context ->
        context.getString(R.string.title_account_notification_deleted)
    }

    is ContactChangeDeletedYouAlert -> { context ->
        context.getString(R.string.title_contact_notification_deleted)
    }

    is ContactChangeBlockedYouAlert -> { context ->
        context.getString(R.string.title_contact_notification_blocked)
    }

    is UpdatedPendingContactOutgoingAcceptedAlert -> { context ->
        context.getString(R.string.title_outgoing_contact_request)
    }

    is UpdatedPendingContactOutgoingDeniedAlert -> { context ->
        context.getString(R.string.title_outgoing_contact_request)
    }

    is UpdatedPendingContactIncomingIgnoredAlert -> { context ->
        context.getString(R.string.title_incoming_contact_request)
    }

    is UpdatedPendingContactIncomingAcceptedAlert -> { context ->
        context.getString(R.string.title_incoming_contact_request)
    }

    is UpdatedPendingContactIncomingDeniedAlert -> { context ->
        context.getString(R.string.title_incoming_contact_request)
    }

    is PaymentSucceededAlert -> { _ ->
        title ?: ""
    }

    is PaymentFailedAlert -> { _ ->
        title ?: ""
    }

    is PaymentReminderAlert -> { _ ->
        title ?: ""
    }

    is TakeDownAlert -> { context ->
        val name = name
        val path = path
        if (path != null && FileUtil.isFile(path)) {
            String.format(
                context.getString(R.string.subtitle_file_takedown_notification),
                Util.toCDATA(name)
            )
        } else {
            String.format(
                context.getString(R.string.subtitle_folder_takedown_notification),
                Util.toCDATA(name)
            )
        }
    }

    is TakeDownReinstatedAlert -> { context ->
        if (path != null && FileUtil.isFile(path)) {
            String.format(
                context.getString(R.string.subtitle_file_takedown_reinstated_notification),
                Util.toCDATA(name)
            )
        } else {
            String.format(
                context.getString(R.string.subtitle_folder_takedown_reinstated_notification),
                Util.toCDATA(name)
            )
        }
    }

    is NewShareAlert -> { context ->
        String.format(
            context.getString(R.string.notification_new_shared_folder),
            contact.getNicknameStringOrEmail(context)
        )
    }

    is DeletedShareAlert -> { context ->
        nodeName?.let { name ->
            String.format(
                context.getString(R.string.notification_left_shared_folder_with_name),
                contact.getNicknameStringOrEmail(context),
                name
            )
        } ?: String.format(
            context.getString(R.string.notification_left_shared_folder),
            contact.getNicknameStringOrEmail(context)
        )
    }

    is RemovedFromShareByOwnerAlert -> { context ->
        String.format(
            context.getString(R.string.notification_deleted_shared_folder),
            contact.getNicknameStringOrEmail(context)
        )
    }

    is NewSharedNodesAlert -> { context ->
        when {
            folderCount > 0 && fileCount > 0 -> {
                val files =
                    context.resources.getQuantityString(
                        R.plurals.num_files_with_parameter,
                        fileCount,
                        fileCount
                    )
                val folders =
                    context.resources.getQuantityString(
                        R.plurals.num_folders_with_parameter,
                        folderCount,
                        folderCount
                    )
                String.format(
                    context.getString(R.string.subtitle_notification_added_folders_and_files),
                    contact.getNicknameStringOrEmail(context),
                    folders,
                    files
                )
            }

            folderCount > 0 -> {
                context.resources.getQuantityString(
                    R.plurals.subtitle_notification_added_folders,
                    folderCount,
                    contact.getNicknameStringOrEmail(context),
                    folderCount
                )
            }

            else -> {
                context.resources.getQuantityString(
                    R.plurals.subtitle_notification_added_files,
                    fileCount,
                    contact.getNicknameStringOrEmail(context),
                    fileCount
                )
            }
        }
    }

    is RemovedSharedNodesAlert -> { context ->
        context.resources.getQuantityString(
            R.plurals.subtitle_notification_deleted_items,
            itemCount,
            contact.getNicknameStringOrEmail(context),
            itemCount
        )
    }

    is ScheduledMeetingAlert -> { _ ->
        title
    }

    is UnknownAlert -> { _ ->
        title ?: ""
    }
}
