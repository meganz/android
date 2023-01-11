package mega.privacy.android.domain.usecase.file

/**
 * Get file versions option
 *
 */
fun interface GetFileVersionsOption {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(foreRefresh: Boolean): Boolean
}