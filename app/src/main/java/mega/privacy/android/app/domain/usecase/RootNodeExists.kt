package mega.privacy.android.app.domain.usecase

/**
 * Root node exists
 *
 */
interface RootNodeExists {
    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(): Boolean
}