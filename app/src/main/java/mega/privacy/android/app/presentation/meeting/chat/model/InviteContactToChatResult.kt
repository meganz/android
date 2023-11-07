package mega.privacy.android.app.presentation.meeting.chat.model

/**
 * Different results of adding contacts to a chat.
 */
sealed interface InviteContactToChatResult {

    /**
     * Multiple contacts have been added successfully.
     *
     * @property success Number of contacts added successfully.
     */
    data class MultipleContactsAdded(val success: Int) : InviteContactToChatResult

    /**
     * Only one contact has been added successfully.
     */
    data object OnlyOneContactAdded : InviteContactToChatResult

    /**
     * One contact already exists in the chat.
     */
    data object AlreadyExistsError : InviteContactToChatResult

    /**
     * Error adding contacts to the chat.
     */
    data object GeneralError : InviteContactToChatResult

    /**
     * Some contacts have been added successfully, some not.
     * 
     * @property success Number of contacts added successfully.
     * @property error Number of contacts not added successfully.
     */
    data class SomeAddedSomeNot(val success: Int, val error: Int) : InviteContactToChatResult
}
