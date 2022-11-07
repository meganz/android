package mega.privacy.android.domain.usecase

/**
 * Is database entry stale
 *
 */
fun interface IsDatabaseEntryStale {
    /**
     * invoke
     * @return true if database stale otherwise false
     */
    suspend operator fun invoke(): Boolean
}