package mega.privacy.android.app.presentation.notification.model.extensions

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.extensions.getQuantityStringOrDefault
import mega.privacy.android.app.presentation.extensions.spanABTextFontColour
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

internal fun UserAlert.title(): (Context) -> CharSequence = when (this) {
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
        this.title ?: ""
    }
    is PaymentFailedAlert -> { _ ->
        this.title ?: ""
    }
    is PaymentReminderAlert -> { _ ->
        this.title ?: ""
    }
    is TakeDownAlert -> { context ->
        val name = this.name
        val path = this.path
        if (path != null && FileUtil.isFile(path)) {
            String.format(context.getString(R.string.subtitle_file_takedown_notification),
                Util.toCDATA(name))
        } else {
            String.format(context.getString(R.string.subtitle_folder_takedown_notification),
                Util.toCDATA(name))
        }.spanABTextFontColour(context)
    }
    is TakeDownReinstatedAlert -> { context ->
        if (path != null && FileUtil.isFile(path)) {
            String.format(context.getString(R.string.subtitle_file_takedown_reinstated_notification),
                Util.toCDATA(name))
        } else {
            String.format(context.getString(R.string.subtitle_folder_takedown_reinstated_notification),
                Util.toCDATA(name))
        }.spanABTextFontColour(context)
    }
    is NewShareAlert -> { context ->
        context.getFormattedStringOrDefault(R.string.notification_new_shared_folder,
            this.contact.getNicknameStringOrEmail(context))
            .spanABTextFontColour(context)
    }
    is DeletedShareAlert -> { context ->
        (this.nodeName?.let {
            context.getFormattedStringOrDefault(R.string.notification_left_shared_folder_with_name,
                this.contact.getNicknameStringOrEmail(context),
                it
            )
        } ?: context.getFormattedStringOrDefault(R.string.notification_left_shared_folder,
            this.contact.getNicknameStringOrEmail(context)
        )).spanABTextFontColour(context)
    }
    is RemovedFromShareByOwnerAlert -> { context ->
        context.getFormattedStringOrDefault(R.string.notification_deleted_shared_folder,
            this.contact.getNicknameStringOrEmail(context)
        ).spanABTextFontColour(context)
    }
    is NewSharedNodesAlert -> { context ->
        when {
            this.folderCount > 0 && this.fileCount > 0 -> {
                val files =
                    context.getQuantityStringOrDefault(R.plurals.num_files_with_parameter,
                        this.fileCount,
                        this.fileCount)
                val folders =
                    context.getQuantityStringOrDefault(R.plurals.num_folders_with_parameter,
                        this.folderCount,
                        this.folderCount)
                context.getFormattedStringOrDefault(R.string.subtitle_notification_added_folders_and_files,
                    this.contact.getNicknameStringOrEmail(context),
                    folders,
                    files
                )
            }
            this.folderCount > 0 -> {
                context.getQuantityStringOrDefault(
                    R.plurals.subtitle_notification_added_folders,
                    this.folderCount,
                    this.contact.getNicknameStringOrEmail(context),
                    this.folderCount
                )
            }
            else -> {
                context.getQuantityStringOrDefault(
                    R.plurals.subtitle_notification_added_files,
                    this.fileCount,
                    this.contact.getNicknameStringOrEmail(context),
                    this.fileCount
                )
            }
        }.spanABTextFontColour(context)
    }
    is RemovedSharedNodesAlert -> { context ->
        context.getQuantityStringOrDefault(
            R.plurals.subtitle_notification_deleted_items,
            this.itemCount,
            this.contact.getNicknameStringOrEmail(context),
            this.itemCount
        ).spanABTextFontColour(context)
    }
    is ScheduledMeetingAlert -> { _ ->
        this.title
    }
    is UnknownAlert -> { _ ->
        this.title ?: ""
    }
}
