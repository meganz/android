package mega.privacy.android.app.domain.usecase

/**
 * Fetch auto accept q r links
 *
 */
interface FetchAutoAcceptQRLinks {
    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(): Boolean
}