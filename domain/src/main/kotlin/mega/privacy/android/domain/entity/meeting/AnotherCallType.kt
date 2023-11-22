package mega.privacy.android.domain.entity.meeting

/**
 *  Enum defining the type of another call.
 */
enum class AnotherCallType {
    /**
     *  No other call exists
     */
    NotCall,

    /**
     *  Another call is in progress
     */
    CallInProgress,

    /**
     * Another call is on hold
     */
    CallOnHold,
}