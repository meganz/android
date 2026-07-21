package mega.privacy.android.domain.entity.transfer

/**
 * Status describing a transfer bandwidth quota warning raised while transferring.
 *
 * Entries are ordered by severity (most severe first) so that the most critical status within a
 * batch of transfer events can be selected via [Enum.ordinal].
 */
enum class TransferOverQuotaStatus {
    /**
     * The transfer bandwidth quota has been exceeded (SDK `API_EOVERQUOTA`).
     */
    OverQuota,

    /**
     * The transfer bandwidth quota is about to be exceeded (SDK `API_EGOINGOVERQUOTA`).
     */
    AlmostOverQuota,
}
