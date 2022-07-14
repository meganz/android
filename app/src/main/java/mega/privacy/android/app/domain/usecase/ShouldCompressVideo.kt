package mega.privacy.android.app.domain.usecase

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
    operator fun invoke(): Boolean
}
