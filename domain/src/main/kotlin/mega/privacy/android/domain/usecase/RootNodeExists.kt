package mega.privacy.android.domain.usecase

/**
 * Root node exists
 *
 */
fun interface RootNodeExists {
    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(): Boolean
}