package mega.privacy.android.domain.exception.chat

/**
 * Participant already exists exception when adding to a chat room
 */
class ParticipantAlreadyExistsException : RuntimeException("Participant already exists")