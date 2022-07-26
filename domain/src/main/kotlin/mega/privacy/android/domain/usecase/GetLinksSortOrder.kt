package mega.privacy.android.domain.usecase

/**
 * Use case interface for getting links sort order
 */
fun interface GetLinksSortOrder {

    /**
     * Get links sort order
     * @return links sort order
     */
    suspend operator fun invoke(): Int
}