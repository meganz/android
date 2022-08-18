package mega.privacy.android.domain.usecase

/**
 * Does media local path exists
 *
 */
fun interface MediaLocalPathExists {

    /**
     * Invoke
     *
     * @return if media local path exists
     */
    suspend operator fun invoke(filePath: String, isSecondary: Boolean): Boolean
}
