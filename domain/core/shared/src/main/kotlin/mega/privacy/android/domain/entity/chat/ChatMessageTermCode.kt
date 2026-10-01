package mega.privacy.android.domain.entity.chat

/**
 * Chat message indicating the termination of a call.
 */
enum class ChatMessageTermCode {
    /**
     * Ended call. Call finished normally.
     */
    ENDED,

    /**
     * Rejected call. Call was rejected by callee.
     */
    REJECTED,

    /**
     * No answer call. Call wasn't answered
     */
    NO_ANSWER,

    /**
     * Failed call. Call finished by an error
     */
    FAILED,

    /**
     * Cancelled call. Call was canceled by caller.
     */
    CANCELLED,

    /**
     * Ended by moderator. Group or meeting call has been ended by moderator
     */
    BY_MODERATOR,
}