package mega.privacy.android.domain.usecase

/**
 * If compressed video is pending
 *
 */
interface CompressedVideoPending {

    /**
     * Invoke
     *
     * @return if compressed video is pending
     */
    operator fun invoke(): Boolean
}
