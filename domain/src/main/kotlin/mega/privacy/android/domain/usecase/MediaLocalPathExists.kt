package mega.privacy.android.domain.usecase

/**
 * Does media local path exists
 *
 */
interface MediaLocalPathExists {

    /**
     * Invoke
     *
     * @return if media local path exists
     */
    operator fun invoke(filePath: String, isSecondary: Boolean): Boolean
}
