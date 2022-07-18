package mega.privacy.android.app.domain.usecase

/**
 * Get video quality setting
 *
 */
interface GetVideoQuality {

    /**
     * Invoke
     *
     * @return video quality setting
     */
    operator fun invoke(): Int
}
