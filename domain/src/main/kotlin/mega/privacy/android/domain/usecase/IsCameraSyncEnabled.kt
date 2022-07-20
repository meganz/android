package mega.privacy.android.domain.usecase

/**
 * Is camera sync enabled
 *
 */
fun interface IsCameraSyncEnabled {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Boolean
}