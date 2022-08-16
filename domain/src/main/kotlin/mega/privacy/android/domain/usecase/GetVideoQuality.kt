package mega.privacy.android.domain.usecase

/**
 * Get video quality setting
 *
 */
fun interface GetVideoQuality {

    /**
     * Invoke
     *
     * @return video quality setting
     */
    suspend operator fun invoke(): Int
}
