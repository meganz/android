package mega.privacy.android.app.domain.usecase

/**
 * Toggle auto accept q r links
 *
 */
fun interface ToggleAutoAcceptQRLinks {
    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(): Boolean
}
