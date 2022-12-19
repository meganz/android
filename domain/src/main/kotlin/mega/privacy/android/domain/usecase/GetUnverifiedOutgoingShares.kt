package mega.privacy.android.domain.usecase

/**
 * GetUnverifiedOutgoingShares Use case
 */
fun interface GetUnverifiedOutgoingShares {

    /**
     * @return Flow of unverified outgoing shares
     */
    suspend operator fun invoke(): Int
}