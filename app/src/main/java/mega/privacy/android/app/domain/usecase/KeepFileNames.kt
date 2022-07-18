package mega.privacy.android.app.domain.usecase

/**
 * Should keep file names
 *
 */
interface KeepFileNames {

    /**
     * Invoke
     *
     * @return whether file name should be kept
     */
    operator fun invoke(): Boolean
}
