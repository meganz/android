package mega.privacy.android.domain.usecase

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
