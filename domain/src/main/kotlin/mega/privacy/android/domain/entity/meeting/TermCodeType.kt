package mega.privacy.android.domain.entity.meeting

/**
 *  Term Code type
 */
enum class TermCodeType {
    /**
     *  Term code invalid
     */
    Invalid,

    /**
     *  Term code Hangup
     */
    Hangup,

    /**
     *  Term code too many participants
     */
    TooManyParticipants,
}