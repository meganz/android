package mega.privacy.android.app.domain.usecase

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