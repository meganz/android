package mega.privacy.android.domain.usecase

/**
 * Fetch auto accept q r links
 *
 */
fun interface FetchAutoAcceptQRLinks {
    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(): Boolean
}