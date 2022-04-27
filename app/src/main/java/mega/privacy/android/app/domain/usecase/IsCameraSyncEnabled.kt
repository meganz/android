package mega.privacy.android.app.domain.usecase

/**
 * Is camera sync enabled
 *
 */
interface IsCameraSyncEnabled {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Boolean
}