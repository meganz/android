package mega.privacy.android.domain.usecase

/**
 * Should compress video
 *
 */
interface ShouldCompressVideo {

    /**
     * Invoke
     *
     * @return whether should compress video
     */
    suspend operator fun invoke(): Boolean
}
