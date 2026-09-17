package mega.privacy.android.domain.entity.transfer

/**
 * Predicted transfer quota state, reported before any of the transfer's files starts.
 *
 * It is a prediction: [Red] does not mean the quota is already exhausted.
 */
enum class TransferQuotaState {
    /**
     * The state could not be determined, including when the API did not answer in time.
     */
    Unknown,

    /**
     * There is enough transfer quota.
     */
    Green,

    /**
     * The transfer completes, but little transfer quota would be left afterwards.
     */
    Orange,

    /**
     * There is not enough transfer quota to transfer everything.
     */
    Red,
}
