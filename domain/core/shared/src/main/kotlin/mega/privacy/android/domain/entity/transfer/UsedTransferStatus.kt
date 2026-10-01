package mega.privacy.android.domain.entity.transfer

/**
 * Status of Transfer Quota used
 */
enum class UsedTransferStatus {
    /**
     * No transfer problems
     */
    NoTransferProblems,

    /**
     * Almost full
     */
    AlmostFull,

    /**
     * Full
     */
    Full
}