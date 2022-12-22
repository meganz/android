package mega.privacy.android.app.presentation.meeting.model

/**
 * Enum class defining the available actions when try to invite a participant
 */
enum class InviteParticipantsAction {

    /**
     * Open add participants no contacts add dialog
     */
    NO_CONTACTS_DIALOG,

    /**
     * Open add participants no contacts left to add dialog
     */
    NO_MORE_CONTACTS_DIALOG,

    /**
     * Open add contact activity
     */
    ADD_CONTACTS
}
