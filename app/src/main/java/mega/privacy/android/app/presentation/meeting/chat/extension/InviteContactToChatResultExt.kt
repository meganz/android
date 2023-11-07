package mega.privacy.android.app.presentation.meeting.chat.extension

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.InviteContactToChatResult


/**
 * Converts the result of adding contacts to a chat to a string.
 *
 * @param context Context to access resources.
 * @return String with the result of adding contacts to a chat.
 */
fun InviteContactToChatResult.toString(context: Context) = when (this) {

    is InviteContactToChatResult.OnlyOneContactAdded -> context.getString(R.string.add_participant_success)
    is InviteContactToChatResult.AlreadyExistsError -> context.getString(R.string.add_participant_error_already_exists)
    is InviteContactToChatResult.GeneralError -> context.getString(R.string.add_participant_error)
    is InviteContactToChatResult.SomeAddedSomeNot -> context.getString(
        R.string.number_no_add_participant_request,
        success,
        error
    )

    is InviteContactToChatResult.MultipleContactsAdded -> context.getString(
        R.string.number_correctly_add_participant,
        success
    )
}

